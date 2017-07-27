package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import org.apache.commons.text.StringEscapeUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

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

public class Users implements AsyncRequest {

	static HashMap<Integer, Long> userIDs = new HashMap<>();
	private String name;
	private String email;
	private String role;
	private Boolean verified;

	public Users() {

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

	public void setName(String name) {
		this.name = name;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
	}
}
