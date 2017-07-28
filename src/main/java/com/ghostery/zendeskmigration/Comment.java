package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import org.apache.commons.text.StringEscapeUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */

public class Comment implements AsyncRequest {

	/**
	 * Get an array of all comments attached to a ticket
	 * @param ticketID      from Evidon account
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected static JSONArray getTicketComments(Integer ticketID) throws ExecutionException, InterruptedException {
		System.out.println("FETCHING COMMENTS...");
		RequestBuilder commentBuild = new RequestBuilder("GET");
		Request commentRequest = commentBuild.setUrl("https://ghostery.zendesk.com/api/v2/tickets/" + ticketID + "/comments.json")
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + evidonCreds)
				.build();
		Future<Response> commentFuture = AsyncRequest.doAsyncRequest(commentRequest);
		Response commentResult = commentFuture.get();

		JSONObject responseCommentObject = new JSONObject(commentResult.getResponseBody());
		JSONArray responseCommentArray = responseCommentObject.getJSONArray("comments");

		JSONArray commentArray = new JSONArray();
		//build new JSONArray for output
		for (int i = 0; i < responseCommentArray.length(); i++) {
			JSONObject obj = new JSONObject();
			JSONObject theComment = responseCommentArray.getJSONObject(i);
			obj.put("body", StringEscapeUtils.escapeHtml4(theComment.getString("body")));
			//obj.put("author_id", userIDs.get(theComment.getInt("author_id")));
			obj.put("created_at", theComment.getString("created_at"));
			obj.put("public", theComment.getBoolean("public"));

			commentArray.put(obj);
		}

		return commentArray;
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

			Future<Response> future = AsyncRequest.doAsyncRequest(request);
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
}
