package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
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
	private JSONArray comment;
	private JSONArray comments;
	private transient Integer legacyID;

	public Ticket() {}

	/**
	 * Retrieve and Post a single ticket by ID,
	 * along with associated users and comments
	 * @param ticketID
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	static void getTicket(Integer ticketID) throws ExecutionException, InterruptedException {
		System.out.println("FETCHING TICKET " + ticketID + "...");
		String evidonZendeskAPI = "https://ghostery.zendesk.com/api/v2/tickets/" + ticketID + ".json?include=users,comment_count";

		//create the HTTP request
		Request request = AsyncRequest.buildEvidonRequest(evidonZendeskAPI);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract tickets and users
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONObject theTicket = responseObject.getJSONObject("ticket");
		JSONArray responseUserArray = responseObject.getJSONArray("users");

		//build new Users and post to Zendesk
		ArrayList<User> userList = User.buildUsers(responseUserArray);
		User.postUsers(userList);

		JSONArray ticketArray = new JSONArray();
		JSONArray tags =  theTicket.getJSONArray("tags");

		//User user = new User();
		Ticket ticket = new Ticket();

		//only get plugin tickets
		if (tags.toString().contains("plugin")) {
			ticket.setSubject(theTicket.getString("subject"));
			ticket.setRequester_id(userIDs.get(theTicket.getInt("requester_id"))); //get new userID from map
			ticket.setStatus((theTicket.getString("status").equals("closed")) ? "solved" : theTicket.getString("status")); //reopen the ticket if it's closed, so we can update it below
			ticket.setCreated_at(theTicket.getString("created_at"));
			ticket.setUpdated_at(theTicket.getString("updated_at"));
			ticket.setLegacyID(theTicket.getInt("id")); //passed to updateTicketComments and then removed before POST
			if (theTicket.get("comment_count") != null) {
				//we can only post 1 comment to a ticket at a time. Here, add the first comment (user request)
				ticket.setComment(Comment.getTicketComments(theTicket.getInt("id")).getJSONObject(0));
			}
		}
		ticket.put(obj);

		System.out.printf( "TICKET: %s", ticket.toString(2) );

		this.postTicket(ticket);
	}

	/**
	 * Get all tickets and sideload users / comment_count
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	void getTickets() throws ExecutionException, InterruptedException {
//		System.out.println("FETCHING TICKETS...");
//		String evidonZendeskAPI = "https://ghostery.zendesk.com/api/v2/tickets.json?include=users,comment_count&per_page=100";//&page=2
//
//		RequestBuilder builder = new RequestBuilder("GET");
//		Request request = builder.setUrl(evidonZendeskAPI)
//				.addHeader("Accept","application/json")
//				.addHeader("Authorization", "Basic " + this.evidonCreds)
//				.build();
//
//		Future<Response> future = this.doAsyncRequest(request);
//		Response result = future.get();
//
//		//Convert to JSON Object and extract data
//		JSONObject responseObject = new JSONObject(result.getResponseBody());
//		JSONArray responseTicketArray = responseObject.getJSONArray("tickets");
//		JSONArray responseUserArray = responseObject.getJSONArray("users");
//
//		//map the new user IDs to the ticket comments. First user is the requester.
//		this.postUsers(responseUserArray);
//
//		JSONArray tickets = new JSONArray();
//		for (int i = 0; i < responseTicketArray.length(); i++) {
//			JSONObject obj = new JSONObject();
//
//			JSONObject theTicket = responseTicketArray.getJSONObject(i);
//			JSONArray tags =  theTicket.getJSONArray("tags");
//			//only get plugin tickets
//			if (tags.toString().contains("plugin")) {
//				obj.put("subject", theTicket.getString("subject"));
//				obj.put("requester_id", userIDs.get(theTicket.getInt("requester_id"))); //get new userID from map
//				obj.put("assignee_id", userIDs.get(theTicket.getInt("assignee_id")));
//				obj.put("status", theTicket.getString("status"));
//				obj.put("created_at", theTicket.getString("created_at"));
//				obj.put("updated_at", theTicket.getString("updated_at"));
//				if (theTicket.get("comment_count") != null) {
//					//create_many.json endpoint allows for comments[]
//					obj.put("comments", this.getTicketComments(theTicket.getInt("id")));
//				}
//			}
//			tickets.put(obj);
//		}
//		System.out.printf( "TICKETS: %s", tickets.toString(2) );
//
//		this.postManyTickets(tickets);
	}

	/**
	 * Post an array of tickets to Zendesk one-by-one
	 * @param tickets
	 */
	private void postTicket(JSONArray tickets) {
//		System.out.println("IMPORTING TICKETS...");
//
//		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/imports/tickets.json";
//
//		for (int i = 0; i < tickets.length(); i++) {
//			//strip out the Evidon ticket ID before POST
//			Integer legacyID = tickets.getJSONObject(i).getInt("legacyID");
//			tickets.getJSONObject(i).remove("legacyID");
//
//			String body = "{\"ticket\":" + tickets.get(i).toString() + "}";
//
//			RequestBuilder builder = new RequestBuilder("POST");
//			Request request = builder.setUrl(ghosteryZendeskAPI)
//					.addHeader("Content-Type", "application/json")
//					.addHeader("Accept", "application/json")
//					.addHeader("Authorization", "Basic " + ghosteryCreds)
//					.setBody(body)
//					.build();
//
//			Future<Response> future = this.doAsyncRequest(request);
//			Response result;
//			try {
//				result = future.get(15, TimeUnit.SECONDS);
//				JSONObject responseObject = new JSONObject(result.getResponseBody());
//				if (result.getStatusCode() <= 201) {
//					Integer ticketID = responseObject.getJSONObject("ticket").getInt("id");
//					System.out.println("Post Ticket: "  + result.getStatusCode() + " " + result.getStatusText() + " ID: " + ticketID);
//					//build out comment for the new ticket
//					this.updateTicketComments(ticketID, legacyID); //new ticket ID, ticketID from original Evidon ticket
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.out.printf("Ticket not uploaded: %s", tickets.get(i).toString());
//				future.cancel(true);
//			}
//		}
	}

	/**
	 * Batch post array of 100 tickets
	 * @param tickets
	 */
	private void postManyTickets(JSONArray tickets) {
//		System.out.println("BULK IMPORTING TICKETS...");
//
//		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/imports/tickets/create_many.json";
//
//		String body = "{\"tickets\":" + tickets.toString() + "}";
//
//		RequestBuilder builder = new RequestBuilder("POST");
//		Request request = builder.setUrl(ghosteryZendeskAPI)
//				.addHeader("Content-Type", "application/json")
//				.addHeader("Accept", "application/json")
//				.addHeader("Authorization", "Basic " + this.ghosteryCreds)
//				.setBody(body)
//				.build();
//
//		Future<Response> future = this.doAsyncRequest(request);
//		Response result;
//		try {
//			result = future.get(15, TimeUnit.SECONDS);
//			if (result.getStatusCode() <= 201) {
//				System.out.println("Batch Post Ticket: "  + result.getStatusCode() + " " + result.getStatusText());
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.out.println("Ticket batch not uploaded");
//			future.cancel(true);
//		}
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public void setRequester_id(Long requester_id) {
		this.requester_id = requester_id;
	}

	public void setAssignee_id(Long assignee_id) {
		this.assignee_id = assignee_id;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	public void setUpdated_at(String updated_at) {
		this.updated_at = updated_at;
	}

	public void setComment(JSONArray comment) {
		this.comment = comment;
	}

	public void setComments(JSONArray comments) {
		this.comments = comments;
	}

	public void setLegacyID(Integer legacyID) {
		this.legacyID = legacyID;
	}
}