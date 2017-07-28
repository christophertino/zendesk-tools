package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import com.google.gson.Gson;
import org.apache.commons.text.StringEscapeUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
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

public class User implements AsyncRequest {

	static HashMap<Integer, Long> userIDs = new HashMap<>();
	private String name;
	private String email;
	private String role;
	private Boolean verified;
	private transient Integer legacyId; //ignore this when serializing

	public User() {}

	/**
	 * Factory function to generate Users from JSONArray
	 * @param users
	 */
	protected static ArrayList<User> buildUsers(JSONArray users) {
		ArrayList<User> output = new ArrayList<>();
		for (int i = 0; i < users.length(); i++) {
			JSONObject user = users.getJSONObject(i);
			User u = new User();

			u.setName(StringEscapeUtils.escapeHtml4(user.getString("name"))); //escape special chars
			u.setEmail(user.getString("email"));
			u.setRole(user.getString("role"));
			u.setVerified(true); //prevent sending verification email
			u.setLegacyId(user.getInt("id"));

			output.add(u);
		}
		return output;
	}

	/**
	 * Post an ArrayList of Users to Zendesk, one-by-one,
	 * mapping old/new userIDs along the way
	 * @param users
	 * @return
	 */
	protected static void postUsers(ArrayList<User> users) {
		System.out.println("POSTING USERS...");

		String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/users/create_or_update.json";

		for (User u : users) {
			//build Users into json for POST
			String body = "{\"user\":" + u.toString() + "}";

			//create the HTTP request
			Request request = AsyncRequest.buildGhosteryRequest("POST", body, ghosteryZendeskAPI);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				JSONObject responseObject = new JSONObject(result.getResponseBody());
				if (result.getStatusCode() <= 201) { //200 update, 201 create
					Long newUserId = responseObject.getJSONObject("user").getLong("id");
					//map old userID to new userID
					System.out.println("Mapping old userID: " + u.legacyId + " to new userID: " + newUserId);
					userIDs.put(u.legacyId, newUserId);
					System.out.println("Post User :" + result.getStatusCode() + " " + result.getStatusText() + " ID: " + userIDs.get(u.legacyId));
				} else {
					System.out.println("Post User error " + result.getStatusCode() + " " + result.getStatusText());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("User not uploaded: %s", u.toString());
				future.cancel(true);
			}
		}
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	private void setName(String name) {
		this.name = name;
	}

	private void setEmail(String email) {
		this.email = email;
	}

	private void setRole(String role) {
		this.role = role;
	}

	private void setVerified(Boolean verified) {
		this.verified = verified;
	}

	private void setLegacyId(Integer legacyId) {
		this.legacyId = legacyId;
	}
}
