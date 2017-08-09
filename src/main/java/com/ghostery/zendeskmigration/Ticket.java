package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import com.google.gson.Gson;
import org.apache.commons.text.StringEscapeUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ghostery.zendeskmigration.User.userIDs;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */

public class Ticket implements AsyncRequest {

	private String subject;
	private Long requester_id;
	private Long assignee_id;
	private String status;
	private String created_at;
	private String updated_at;
	private String comment;
	private ArrayList<Comment> comments;
	private transient Integer legacyID;

	public Ticket() {}

	/**
	 * Retrieve a single ticket by ID,
	 * along with associated users and comments
	 * @param ticketID
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected static Ticket getTicket(Integer ticketID) throws ExecutionException, InterruptedException {
		System.out.println("GET TICKET " + ticketID + "...");
		String evidonZendeskAPI = "https://ghostery.zendesk.com/api/v2/tickets/" + ticketID + ".json?include=users,comment_count";

		//create the HTTP request
		Request request = AsyncRequest.buildEvidonRequest(evidonZendeskAPI);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract tickets and users
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONObject responseTicket = responseObject.getJSONObject("ticket");
		JSONArray responseUsers = responseObject.getJSONArray("users");

		//build new Users and post to Zendesk
		ArrayList<User> userList = User.buildUsers(responseUsers);
		User.postUsers(userList);

		//only get plugin tickets
		if (responseTicket.getJSONArray("tags").toString().contains("plugin")) {
			Ticket ticket = buildTicket(responseTicket);
			System.out.println("TICKET: " + ticket.toString());
			return ticket;
		} else {
			return null;
		}
	}

	/**
	 * Factory function to build a single Ticket
	 * @param ticketObj
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static Ticket buildTicket(JSONObject ticketObj) throws ExecutionException, InterruptedException {
		Ticket t = new Ticket();

		t.setSubject(ticketObj.getString("subject"));
		t.setRequester_id(userIDs.get(ticketObj.getInt("requester_id"))); //get new userID from map
		t.setAssignee_id(userIDs.get(ticketObj.optInt("assignee_id")));
		t.setStatus((ticketObj.getString("status").equals("closed")) ? "solved" : ticketObj.getString("status")); //reopen the ticket if it's closed, so we can update it below
		t.setCreated_at(ticketObj.getString("created_at"));
		t.setUpdated_at(ticketObj.getString("updated_at"));
		t.setLegacyID(ticketObj.getInt("id")); //passed to updateComments and then removed before POST

		if (ticketObj.get("comment_count") != null) {
			//we can only post 1 comment to a ticket at a time. Here, add the first comment (user request)
			Comment firstComment = Comment.getComments(ticketObj.getInt("id")).get(0);
			t.setComment(firstComment.toString());
		}

		return t;
	}

	/**
	 * POST a single ticket to Zendesk
	 * @param ticket
	 */
	protected static void postTicket(Ticket ticket) {
		System.out.println("POST TICKET...");

		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/imports/tickets.json";

		//build Ticket into json
		String body = "{\"ticket\":" + ticket.toString() + "}";

		//create the HTTP request
		Request request = AsyncRequest.buildGhosteryRequest("POST", body, ghosteryZendeskAPI);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result;

		try {
			result = future.get(15, TimeUnit.SECONDS);
			JSONObject responseObject = new JSONObject(result.getResponseBody());
			if (result.getStatusCode() <= 201) {
				Integer ticketID = responseObject.getJSONObject("ticket").getInt("id");
				System.out.println("Post Ticket: "  + result.getStatusCode() + " " + result.getStatusText() + " ID: " + ticketID);
				//build out comment for the new ticket
				Comment.updateComments(ticketID, ticket.legacyID); //new ticket ID, ticketID from original Evidon ticket
			} else {
				System.out.println("Post Ticket Error: "  + result.getStatusCode() + " " + result.getStatusText());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.printf("Ticket not uploaded: %s", ticket.toString());
			future.cancel(true);
		}
	}

	/**
	 * Retrieve a batch of 100 tickets with sideload users / comment_count, and POST.
	 * Will paginate through all tickets in the account
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected static void getTickets() throws ExecutionException, InterruptedException {
		Integer currentPage = 1;
		String evidonZendeskAPI = "https://ghostery.zendesk.com/api/v2/tickets.json?include=users,comment_count&per_page=100&page=" + currentPage;

		while(evidonZendeskAPI != null) {
			System.out.println("GETTING TICKETS, PAGE " + currentPage + "...");

			//create the HTTP request
			Request request = AsyncRequest.buildEvidonRequest(evidonZendeskAPI);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result = future.get();

			//Convert response to JSON Object and extract tickets and users
			JSONObject responseObject = new JSONObject(result.getResponseBody());
			JSONArray responseTicketArray = responseObject.getJSONArray("tickets");
			JSONArray responseUserArray = responseObject.getJSONArray("users");

			//build new Users and post to Zendesk
			ArrayList<User> userList = User.buildUsers(responseUserArray);
			User.postUsers(userList);

			ArrayList<Ticket> tickets = buildTickets(responseTicketArray);
			System.out.println("TICKETS: " + tickets.toString());

			postTickets(tickets);

			//pause for 30sec so we don't go over the API rate limit
			Thread.sleep(30000);
			currentPage++;
			evidonZendeskAPI = responseObject.optString("next_page", null);
		}
	}

	/**
	 * Factory function to generate an ArrayList of Tickets
	 * @param tickets
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static ArrayList<Ticket> buildTickets(JSONArray tickets) throws ExecutionException, InterruptedException {
		ArrayList<Ticket> output = new ArrayList<>();
		for (int i = 0; i < tickets.length(); i++) {
			JSONObject ticketObj = tickets.getJSONObject(i);

			//only get plugin tickets
			if (ticketObj.getJSONArray("tags").toString().contains("plugin")){
				Ticket t = new Ticket();

				t.setSubject(StringEscapeUtils.escapeHtml4(ticketObj.getString("subject")));
				t.setRequester_id(userIDs.get(ticketObj.getInt("requester_id"))); //get new userID from map
				t.setAssignee_id(userIDs.get(ticketObj.optInt("assignee_id")));
				t.setStatus(ticketObj.getString("status"));
				t.setCreated_at(ticketObj.getString("created_at"));
				t.setUpdated_at(ticketObj.getString("updated_at"));

				if (ticketObj.get("comment_count") != null) {
					//create_many.json endpoint allows for comments[]
					t.setComments(Comment.getComments(ticketObj.getInt("id")));
				}

				output.add(t);
			}
		}
		return output;
	}

	/**
	 * Batch POST array of up to 100 tickets
	 * @param tickets
	 */
	private static void postTickets(ArrayList<Ticket> tickets) {
		System.out.println("BULK IMPORTING TICKETS...");

		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/imports/tickets/create_many.json";

		String body = "{\"tickets\":" + tickets.toString() + "}";

		//create the HTTP request
		Request request = AsyncRequest.buildGhosteryRequest("POST", body, ghosteryZendeskAPI);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result;

		try {
			result = future.get(15, TimeUnit.SECONDS);
			System.out.println("Batch Post Ticket: "  + result.getStatusCode() + " " + result.getStatusText());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Ticket batch not uploaded");
			future.cancel(true);
		}
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this).replace("is_public", "public"); //for Comments
	}

	private void setSubject(String subject) {
		this.subject = subject;
	}

	private void setRequester_id(Long requester_id) {
		this.requester_id = requester_id;
	}

	private void setAssignee_id(Long assignee_id) {
		this.assignee_id = assignee_id;
	}

	private void setStatus(String status) {
		this.status = status;
	}

	private void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	private void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}

	private void setComment(String comment) {
		this.comment = comment;
	}

	private void setComments(ArrayList<Comment> comments) {
		this.comments = comments;
	}

	private void setLegacyID(Integer legacyID) {
		this.legacyID = legacyID;
	}
}
