package com.ghostery.zendesktools.actions;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.ghostery.zendesktools.models.User;
import org.apache.commons.text.StringEscapeUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ghostery.zendesktools.interfaces.Constants.CURRENT_API_URL;

/**
 * User Controller
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */
public class UserController {
	/**
	 * Factory function to generate Users from JSONArray
	 * @param users
	 */
	public static ArrayList<User> buildUsers(JSONArray users) {
		ArrayList<User> output = new ArrayList<>();
		for (int i = 0; i < users.length(); i++) {
			JSONObject user = users.getJSONObject(i);
			User u = new User(
					StringEscapeUtils.escapeHtml4(user.getString("name")), //escape special chars
					user.getString("email"),
					user.getString("role"),
					true, //prevent sending verification email
					user.getInt("id")
			);
			output.add(u);
		}
		return output;
	}

	/**
	 * POST an ArrayList of Users to Zendesk, one-by-one,
	 * mapping old/new userIDs along the way
	 * @param users
	 * @return
	 */
	public static void postUsers(ArrayList<User> users) {
		System.out.println("POSTING USERS...");

		String currentURL = CURRENT_API_URL + "/users/create_or_update.json";

		for (User u : users) {
			//build Users into json for POST
			String body = "{\"user\":" + u.toString() + "}";

			//create the HTTP request
			Request request = AsyncRequest.buildCurrentUpdateRequest("POST", body, currentURL);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				JSONObject responseObject = new JSONObject(result.getResponseBody());
				if (result.getStatusCode() <= 201) { //200 update, 201 create
					Long newUserId = responseObject.getJSONObject("user").getLong("id");
					//map old userID to new userID
					System.out.println("Mapping old userID: " + u.getLegacyId() + " to new userID: " + newUserId);
					User.userIDs.put(u.getLegacyId(), newUserId);
					System.out.println("Post User:" + result.getStatusCode() + " " + result.getStatusText() + " ID: " + User.userIDs.get(u.getLegacyId()));
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
}
