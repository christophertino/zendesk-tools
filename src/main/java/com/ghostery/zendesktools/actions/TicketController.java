package com.ghostery.zendesktools.actions;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.ghostery.zendesktools.models.Comment;
import com.ghostery.zendesktools.models.Ticket;
import com.ghostery.zendesktools.models.User;
import org.apache.commons.text.StringEscapeUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ghostery.zendesktools.interfaces.Constants.*;
import static com.ghostery.zendesktools.interfaces.Constants.CURRENT_API_URL;
import static com.ghostery.zendesktools.models.User.userIDs;

/**
 * Ticket Controller
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */
public class TicketController {
	/**
	 * Retrieve a single ticket by ID,
	 * along with associated users and comments
	 * @param ticketID
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public Ticket getTicket(Integer ticketID) throws ExecutionException, InterruptedException {
		System.out.println("GET TICKET " + ticketID + "...");
		String legacyURL = LEGACY_API_URL + "/tickets/" + ticketID + ".json?include=users,comment_count";

		//create the HTTP request
		Request request = AsyncRequest.buildLegacyRequest(legacyURL);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract tickets and users
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONObject responseTicket = responseObject.getJSONObject("ticket");
		JSONArray responseUsers = responseObject.getJSONArray("users");

		//build new Users and post to Zendesk
		ArrayList<User> userList = UserController.buildUsers(responseUsers);
		UserController.postUsers(userList);

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
	private Ticket buildTicket(JSONObject ticketObj) throws ExecutionException, InterruptedException {
		Ticket t = new Ticket(
			ticketObj.getString("subject"),
			userIDs.get(ticketObj.getInt("requester_id")), //get new userID from map
			userIDs.get(ticketObj.optInt("assignee_id")),
			ticketObj.getString("status").equals("closed") ? "solved" : ticketObj.getString("status"), //reopen the ticket if it's closed, so we can update it below
			ticketObj.getString("created_at"),
			ticketObj.getString("updated_at"),
			ticketObj.getInt("id") //passed to updateComments and then removed before POST
		);

		if (ticketObj.get("comment_count") != null) {
			//we can only post 1 comment to a ticket at a time. Here, add the first comment (user request)
			Comment firstComment = CommentController.getComments(ticketObj.getInt("id")).get(0);
			t.setComment(firstComment.toString());
		}

		return t;
	}

	/**
	 * POST a single ticket to Zendesk
	 * @param ticket
	 */
	public void postTicket(Ticket ticket) {
		System.out.println("POST TICKET...");

		String currentURL = CURRENT_API_URL + "/imports/tickets.json";

		//build Ticket into json
		String body = "{\"ticket\":" + ticket.toString() + "}";

		//create the HTTP request
		Request request = AsyncRequest.buildCurrentUpdateRequest("POST", body, currentURL);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result;

		try {
			result = future.get(15, TimeUnit.SECONDS);
			JSONObject responseObject = new JSONObject(result.getResponseBody());
			if (result.getStatusCode() <= 201) {
				Integer ticketID = responseObject.getJSONObject("ticket").getInt("id");
				System.out.println("Post Ticket: "  + result.getStatusCode() + " " + result.getStatusText() + " ID: " + ticketID);
				//build out comment for the new ticket
				CommentController.updateComments(ticketID, ticket.getLegacyID()); //new ticket ID, ticketID from original ticket
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
	public void getTickets() throws ExecutionException, InterruptedException {
		Integer currentPage = 1;
		String legacyURL = LEGACY_API_URL + "/tickets.json?include=users,comment_count&per_page=100&page=" + currentPage;

		while(legacyURL != null) {
			System.out.println("GETTING TICKETS, PAGE " + currentPage + "...");

			//create the HTTP request
			Request request = AsyncRequest.buildLegacyRequest(legacyURL);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result = future.get();

			//Convert response to JSON Object and extract tickets and users
			JSONObject responseObject = new JSONObject(result.getResponseBody());
			JSONArray responseTicketArray = responseObject.getJSONArray("tickets");
			JSONArray responseUserArray = responseObject.getJSONArray("users");

			//build new Users and post to Zendesk
			ArrayList<User> userList = UserController.buildUsers(responseUserArray);
			UserController.postUsers(userList);

			ArrayList<Ticket> tickets = buildTickets(responseTicketArray);
			System.out.println("TICKETS: " + tickets.toString());

			postTickets(tickets);

			//pause for 30sec so we don't go over the API rate limit
			Thread.sleep(30000);
			currentPage++;
			legacyURL = responseObject.optString("next_page", null);
		}
	}

	/**
	 * Factory function to generate an ArrayList of Tickets
	 * @param tickets
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private ArrayList<Ticket> buildTickets(JSONArray tickets) throws ExecutionException, InterruptedException {
		ArrayList<Ticket> output = new ArrayList<>();
		for (int i = 0; i < tickets.length(); i++) {
			JSONObject ticketObj = tickets.getJSONObject(i);

			//only get plugin tickets
			if (ticketObj.getJSONArray("tags").toString().contains("plugin")){
				Ticket t = new Ticket(
						StringEscapeUtils.escapeHtml4(ticketObj.getString("subject")),
						userIDs.get(ticketObj.getInt("requester_id")), //get new userID from map
						userIDs.get(ticketObj.optInt("assignee_id")),
						ticketObj.getString("status"),
						ticketObj.getString("created_at"),
						ticketObj.getString("updated_at")
				);

				if (ticketObj.get("comment_count") != null) {
					//create_many.json endpoint allows for comments[]
					t.setComments(CommentController.getComments(ticketObj.getInt("id")));
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
	private void postTickets(ArrayList<Ticket> tickets) {
		System.out.println("BULK IMPORTING TICKETS...");

		String currentURL = CURRENT_API_URL + "/imports/tickets/create_many.json";

		String body = "{\"tickets\":" + tickets.toString() + "}";

		//create the HTTP request
		Request request = AsyncRequest.buildCurrentUpdateRequest("POST", body, currentURL);
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

	/**
	 * Retrieve only NEW tickets using the Ticket View API
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static void getNewTickets() throws ExecutionException, InterruptedException {
		Integer currentPage = 1;
		String currentURL = CURRENT_API_URL + "/views/" + NEW_TICKETS_VIEW_ID + "/tickets.json?&per_page=100&page=" + currentPage;

		while(currentURL != null) {
			System.out.println("GETTING NEW TICKETS, PAGE " + currentPage + "...");

			//create the HTTP request
			Request request = AsyncRequest.buildCurrentUpdateRequest("GET", "", currentURL);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result = future.get();

			//Convert response to JSON Object and extract tickets and users
			JSONObject responseObject = new JSONObject(result.getResponseBody());
			JSONArray responseTicketArray = responseObject.getJSONArray("tickets");

			ArrayList<Ticket> tickets = buildNewTickets(responseTicketArray);
			System.out.println("TICKETS: " + tickets.toString());

			putNewTickets(tickets);

			currentPage++;
			currentURL = responseObject.optString("next_page", null);
		}
	}

	/**
	 * Factory function to generate an ArrayList of new Tickets and
	 * add new properties
	 * @param tickets
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private static ArrayList<Ticket> buildNewTickets(JSONArray tickets) throws ExecutionException, InterruptedException {
		ArrayList<Ticket> output = new ArrayList<>();
		for (int i = 0; i < tickets.length(); i++) {
			JSONObject ticketObj = tickets.getJSONObject(i);

			//only update tickets that do not have a sharing agreement
			if (ticketObj.optJSONArray("sharing_agreement_ids").length() == 0) {
				Ticket t = new Ticket(
						ticketObj.getInt("id"),
						"new",
						SHARING_ID
				);
				output.add(t);
			}
		}
		return output;
	}

	/**
	 * Batch PUT array of up to 100 new tickets
	 * @param tickets
	 */
	private static void putNewTickets(ArrayList<Ticket> tickets) {
		if (tickets.isEmpty()) {
			System.out.println("No new tickets to update.");
			return;
		}

		System.out.println("BULK UPDATING NEW TICKETS...");

		String currentURL = CURRENT_API_URL + "/tickets/update_many.json";

		String body = "{\"tickets\":" + tickets.toString() + "}";

		//create the HTTP request
		Request request = AsyncRequest.buildCurrentUpdateRequest("PUT", body, currentURL);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result;

		try {
			result = future.get(15, TimeUnit.SECONDS);
			System.out.println("Batch PUT Ticket: "  + result.getStatusCode() + " " + result.getStatusText());
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Ticket batch not uploaded");
			future.cancel(true);
		}
	}
}
