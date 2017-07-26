package com.ghostery.zendeskmigration;

import org.asynchttpclient.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.text.StringEscapeUtils;
import static com.ghostery.zendeskmigration.Constants.*;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */

public class ZendeskMigration implements AsyncRequest {

	private final String evidonCreds = Base64.getEncoder().encodeToString((EVIDON_USER + "/token:" + EVIDON_TOKEN).getBytes(StandardCharsets.UTF_8));
	private final String ghosteryCreds = Base64.getEncoder().encodeToString((GHOSTERY_USER + "/token:" + GHOSTERY_TOKEN).getBytes(StandardCharsets.UTF_8));
	private HashMap<Integer, Long> userIDs = new HashMap<>();

	private ZendeskMigration() throws ExecutionException, InterruptedException {
//		HelpCenter hc = new HelpCenter();
//		hc.getHelpCenterContent("categories");
//		hc.getHelpCenterContent("sections");
//		hc.getHelpCenterContent("articles");

		//this.getTicket(10629);
		this.getTickets();
	}

	/**
	 * Retrieve and Post a single ticket by ID
	 * @param ticketID
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void getTicket(Integer ticketID) throws ExecutionException, InterruptedException {
		System.out.println("FETCHING TICKET " + ticketID + "...");
		String evidonZendeskAPI = "https://ghostery.zendesk.com/api/v2/tickets/" + ticketID + ".json?include=users,comment_count";

		RequestBuilder builder = new RequestBuilder("GET");
		Request request = builder.setUrl(evidonZendeskAPI)
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + evidonCreds)
				.build();

		Future<Response> future = this.doAsyncRequest(request);
		Response result = future.get();

		//Convert to JSON Object and extract data
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONObject theTicket = responseObject.getJSONObject("ticket");
		JSONArray responseUserArray = responseObject.getJSONArray("users");

		//map the new user IDs to the ticket comments. First user is the requester.
		this.postUsers(responseUserArray);

		JSONArray ticket = new JSONArray();
		JSONObject obj = new JSONObject();

		JSONArray tags =  theTicket.getJSONArray("tags");
		//only get plugin tickets
		if (tags.toString().contains("plugin")) {
			obj.put("subject", theTicket.getString("subject"));
			obj.put("requester_id", userIDs.get(theTicket.getInt("requester_id"))); //get new userID from map
			obj.put("status", (theTicket.getString("status").equals("closed")) ? "solved" : theTicket.getString("status")); //reopen the ticket if it's closed, so we can update it below
			obj.put("created_at", theTicket.getString("created_at"));
			obj.put("updated_at", theTicket.getString("updated_at"));
			obj.put("legacyID", theTicket.getInt("id")); //passed to updateTicketComments and then removed before POST
			if (theTicket.get("comment_count") != null) {
				//we can only post 1 comment to a ticket at a time. Here, add the first comment (user request)
				obj.put("comment", this.getTicketComments(theTicket.getInt("id")).getJSONObject(0));
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
	private void getTickets() throws ExecutionException, InterruptedException {
		System.out.println("FETCHING TICKETS...");
		String evidonZendeskAPI = "https://ghostery.zendesk.com/api/v2/tickets.json?include=users,comment_count&per_page=100";//&page=2

		RequestBuilder builder = new RequestBuilder("GET");
		Request request = builder.setUrl(evidonZendeskAPI)
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + evidonCreds)
				.build();

		Future<Response> future = this.doAsyncRequest(request);
		Response result = future.get();

		//Convert to JSON Object and extract data
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONArray responseTicketArray = responseObject.getJSONArray("tickets");
		JSONArray responseUserArray = responseObject.getJSONArray("users");

		//map the new user IDs to the ticket comments. First user is the requester.
		this.postUsers(responseUserArray);

		JSONArray tickets = new JSONArray();
		for (int i = 0; i < responseTicketArray.length(); i++) {
			JSONObject obj = new JSONObject();

			JSONObject theTicket = responseTicketArray.getJSONObject(i);
			JSONArray tags =  theTicket.getJSONArray("tags");
			//only get plugin tickets
			if (tags.toString().contains("plugin")) {
				obj.put("subject", theTicket.getString("subject"));
				obj.put("requester_id", userIDs.get(theTicket.getInt("requester_id"))); //get new userID from map
				obj.put("assignee_id", userIDs.get(theTicket.getInt("assignee_id")));
				obj.put("status", theTicket.getString("status"));
				obj.put("created_at", theTicket.getString("created_at"));
				obj.put("updated_at", theTicket.getString("updated_at"));
				if (theTicket.get("comment_count") != null) {
					//create_many.json endpoint allows for comments[]
					obj.put("comments", this.getTicketComments(theTicket.getInt("id")));
				}
			}
			tickets.put(obj);
		}
		System.out.printf( "TICKETS: %s", tickets.toString(2) );

		this.postManyTickets(tickets);
	}

	/**
	 * Get an array of all comments attached to a ticket
	 * @param ticketID      from Evidon account
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private JSONArray getTicketComments(Integer ticketID) throws ExecutionException, InterruptedException {
		System.out.println("FETCHING COMMENTS...");
		RequestBuilder commentBuild = new RequestBuilder("GET");
		Request commentRequest = commentBuild.setUrl("https://ghostery.zendesk.com/api/v2/tickets/" + ticketID + "/comments.json")
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + evidonCreds)
				.build();
		Future<Response> commentFuture = this.doAsyncRequest(commentRequest);
		Response commentResult = commentFuture.get();

		JSONObject responseCommentObject = new JSONObject(commentResult.getResponseBody());
		JSONArray responseCommentArray = responseCommentObject.getJSONArray("comments");

		JSONArray commentArray = new JSONArray();
		//build new JSONArray for output
		for (int i = 0; i < responseCommentArray.length(); i++) {
			JSONObject obj = new JSONObject();
			JSONObject theComment = responseCommentArray.getJSONObject(i);
			obj.put("body", StringEscapeUtils.escapeHtml4(theComment.getString("body")));
			obj.put("author_id", userIDs.get(theComment.getInt("author_id")));
			obj.put("created_at", theComment.getString("created_at"));
			obj.put("public", theComment.getBoolean("public"));

			commentArray.put(obj);
		}

		return commentArray;
	}

	/**
	 * Post an array of tickets to Zendesk one-by-one
	 * @param tickets
	 */
	private void postTicket(JSONArray tickets) {
		System.out.println("IMPORTING TICKETS...");

		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/imports/tickets.json";

		for (int i = 0; i < tickets.length(); i++) {
			//strip out the Evidon ticket ID before POST
			Integer legacyID = tickets.getJSONObject(i).getInt("legacyID");
			tickets.getJSONObject(i).remove("legacyID");

			String body = "{\"ticket\":" + tickets.get(i).toString() + "}";

			RequestBuilder builder = new RequestBuilder("POST");
			Request request = builder.setUrl(ghosteryZendeskAPI)
					.addHeader("Content-Type", "application/json")
					.addHeader("Accept", "application/json")
					.addHeader("Authorization", "Basic " + ghosteryCreds)
					.setBody(body)
					.build();

			Future<Response> future = this.doAsyncRequest(request);
			Response result;
			try {
				result = future.get(15, TimeUnit.SECONDS);
				JSONObject responseObject = new JSONObject(result.getResponseBody());
				if (result.getStatusCode() <= 201) {
					Integer ticketID = responseObject.getJSONObject("ticket").getInt("id");
					System.out.println("Post Ticket: "  + result.getStatusCode() + " " + result.getStatusText() + " ID: " + ticketID);
					//build out comment for the new ticket
					this.updateTicketComments(ticketID, legacyID); //new ticket ID, ticketID from original Evidon ticket
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Ticket not uploaded: %s", tickets.get(i).toString());
				future.cancel(true);
			}
		}
	}

	/**
	 * Batch post array of 100 tickets
	 * @param tickets
	 */
	private void postManyTickets(JSONArray tickets) {
		System.out.println("BULK IMPORTING TICKETS...");

		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/imports/tickets/create_many.json";

		String body = "{\"tickets\":" + tickets.toString() + "}";

		RequestBuilder builder = new RequestBuilder("POST");
		Request request = builder.setUrl(ghosteryZendeskAPI)
				.addHeader("Content-Type", "application/json")
				.addHeader("Accept", "application/json")
				.addHeader("Authorization", "Basic " + ghosteryCreds)
				.setBody(body)
				.build();

		Future<Response> future = this.doAsyncRequest(request);
		Response result;
		try {
			result = future.get(15, TimeUnit.SECONDS);
			if (result.getStatusCode() <= 201) {
				System.out.println("Batch Post Tickets: "  + result.getStatusCode() + " " + result.getStatusText());
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Ticket batch not uploaded");
			future.cancel(true);
		}

	}

	/**
	 * PUT each additional ticket comment onto the new ticket, one-by-one
	 * @param newTicketID
	 * @param oldTicketID
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void updateTicketComments(Integer newTicketID, Integer oldTicketID) throws ExecutionException, InterruptedException {
		System.out.println("UPDATING TICKET COMMENTS...");

		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/tickets/" + newTicketID + ".json";

		JSONArray ticketComments = this.getTicketComments(oldTicketID);
		//start at index 1, since the first comment was added during ticket creation
		for (int i = 1; i < ticketComments.length(); i++) {
			String body = "{\"ticket\": {\"comment\":" + ticketComments.getJSONObject(i).toString() + "}}";

			RequestBuilder builder = new RequestBuilder("PUT");
			Request request = builder.setUrl(ghosteryZendeskAPI)
					.addHeader("Content-Type", "application/json")
					.addHeader("Accept", "application/json")
					.addHeader("Authorization", "Basic " + ghosteryCreds)
					.setBody(body)
					.build();

			Future<Response> future = this.doAsyncRequest(request);
			Response result;
			try {
				result = future.get(15, TimeUnit.SECONDS);
				System.out.println("Update Ticket: " + result.getStatusCode() + " " + result.getStatusText());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Ticket not uploaded: %s", ticketComments.get(i).toString());
				future.cancel(true);
			}
		}
	}

	/**
	 * Post an array of Users to Zendesk
	 * @param users
	 * @return
	 */
	private void postUsers(JSONArray users) {
		System.out.println("IMPORTING USERS...");

		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/users/create_or_update.json";

		for (int i = 0; i < users.length(); i++) {
			JSONObject user = users.getJSONObject(i);
			JSONObject userOutput = new JSONObject();
			userOutput.put("name", StringEscapeUtils.escapeHtml4(user.getString("name"))); //escape special chars
			userOutput.put("email", user.getString("email"));
			userOutput.put("role", user.getString("role"));
			userOutput.put("verified", true); //prevent sending verification email

			String body = "{\"user\":" + userOutput.toString() + "}";

			RequestBuilder builder = new RequestBuilder("POST");
			Request request = builder.setUrl(ghosteryZendeskAPI)
					.addHeader("Content-Type", "application/json")
					.addHeader("Accept", "application/json")
					.addHeader("Authorization", "Basic " + ghosteryCreds)
					.setBody(body)
					.build();

			Future<Response> future = this.doAsyncRequest(request);
			Response result;
			try {
				result = future.get(15, TimeUnit.SECONDS);
				JSONObject responseObject = new JSONObject(result.getResponseBody());
				if (result.getStatusCode() <= 201) { //200 update, 201 create
					//map old userID to new userID
					System.out.println("Mapping old userID: " + user.getInt("id") + " to new userID: " + responseObject.getJSONObject("user").getLong("id"));
					userIDs.put(user.getInt("id"), responseObject.getJSONObject("user").getLong("id"));
					System.out.println("Post Users :" + result.getStatusCode() + " " + result.getStatusText() + " ID: " + userIDs.get(user.getInt("id")));
				} else {
					System.out.println("Post Users error " + result.getStatusCode() + " " + result.getStatusText());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("User not uploaded: %s", userOutput.toString());
				future.cancel(true);
			}
		}
	}

	public static void main(String[] args) {
		try {
			new ZendeskMigration();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
