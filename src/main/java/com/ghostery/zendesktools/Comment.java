package com.ghostery.zendesktools;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
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

import static com.ghostery.zendesktools.interfaces.Constants.CURRENT_API_URL;
import static com.ghostery.zendesktools.interfaces.Constants.LEGACY_API_URL;

/**
 * Ghostery Zendesk Tools
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */

public class Comment implements AsyncRequest {

	private String body;
	private Long author_id;
	private String created_at;
	private Boolean is_public;

	private Comment() {}

	/**
	 * Get an array of all Comments attached to a ticket
	 * @param ticketID      from legacy account
	 * @return
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected static ArrayList<Comment> getComments(Integer ticketID) throws ExecutionException, InterruptedException {
		System.out.println("GETTING COMMENTS FOR TICKET " + ticketID + "...");
		String legacyURL = LEGACY_API_URL + "/tickets/" + ticketID + "/comments.json";

		//create the HTTP request
		Request request = AsyncRequest.buildLegacyRequest(legacyURL);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract comments
		JSONObject responseCommentObject = new JSONObject(result.getResponseBody());
		JSONArray responseCommentArray = responseCommentObject.getJSONArray("comments");

		//build new Comments
		return buildComments(responseCommentArray);
	}

	/**
	 * Factory function to generate Comments from JSONArray
	 * @param comments
	 * @return
	 */
	private static ArrayList<Comment> buildComments(JSONArray comments) {
		ArrayList<Comment> output = new ArrayList<>();
		for (int i = 0; i < comments.length(); i++) {
			JSONObject comment = comments.getJSONObject(i);
			Comment c = new Comment();

			c.setBody(StringEscapeUtils.escapeHtml4(comment.getString("body")));
			c.setAuthor_id(User.userIDs.get(comment.getInt("author_id")));
			c.setCreated_at(comment.getString("created_at"));
			c.setIs_public(comment.getBoolean("public"));

			output.add(c);
		}
		return output;
	}

	/**
	 * PUT each additional ticket comment onto the new ticket, one-by-one
	 * @param newTicketID
	 * @param oldTicketID
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected static void updateComments(Integer newTicketID, Integer oldTicketID) throws ExecutionException, InterruptedException {
		System.out.println("UPDATING TICKET COMMENTS...");

		String currentURL = CURRENT_API_URL + "/tickets/" + newTicketID + ".json";

		ArrayList<Comment> comments = getComments(oldTicketID);
		//start at index 1, since the first comment was added during ticket creation
		for (int i = 1; i < comments.size(); i++) {
			String body = "{\"ticket\": {\"comment\":" + comments.get(i).toString() + "}}";

			//create the HTTP request
			Request request = AsyncRequest.buildCurrentUpdateRequest("PUT", body, currentURL);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				System.out.println("Update Ticket: " + result.getStatusCode() + " " + result.getStatusText());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Ticket not uploaded: %s", comments.get(i).toString());
				future.cancel(true);
			}
		}
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this).replace("is_public", "public");
	}

	private void setBody(String body) {
		this.body = body;
	}

	private void setAuthor_id(Long author_id) {
		this.author_id = author_id;
	}

	private void setCreated_at(String created_at) {
		this.created_at = created_at;
	}

	private void setIs_public(Boolean is_public) {
		this.is_public = is_public;
	}
}