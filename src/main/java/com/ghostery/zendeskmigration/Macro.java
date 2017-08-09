package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.*;
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

public class Macro implements AsyncRequest {

	private String title;
	private String description;
	private Boolean active;
	private List<Map<String, Object>> actions;

	private Macro() {}

	/**
	 * Retrieve a list of macros from Zendesk
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected static ArrayList<Macro> getMacros() throws ExecutionException, InterruptedException {
		System.out.println("GETTING MACROS...");
		String evidonZendeskAPI = "https://evidon.zendesk.com/api/v2/macros.json";

		//create the HTTP request
		Request request = AsyncRequest.buildEvidonRequest(evidonZendeskAPI);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract macros
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONArray responseMacroArray = responseObject.getJSONArray("macros");

		ArrayList<Macro> macros = buildMacros(responseMacroArray);
		System.out.println("MACROS: " + macros.toString());

		return macros;
	}

	/**
	 * Factory function to generate Macros from JSONArray
	 * @param macros
	 */
	private static ArrayList<Macro> buildMacros(JSONArray macros) {
		ArrayList<Macro> output = new ArrayList<>();
		for (int i = 0; i < macros.length(); i++) {
			JSONObject macro = macros.getJSONObject(i);
			Macro m = new Macro();

			m.setTitle(macro.getString("title"));
			m.setDescription(macro.optString("description", null));
			m.setActive(macro.getBoolean("active"));
			m.setActions(buildActionList(macro.getJSONArray("actions")));

			output.add(m);
		}
		return output;
	}

	/**
	 * POST an ArrayList of Macros to Zendesk, one-by-one
	 * @param macros
	 * @return
	 */
	protected static void postMacros(ArrayList<Macro> macros) {
		System.out.println("POSTING MACROS...");

		String ghosteryZendeskAPI = "https://ghostery.zendesk.com/api/v2/macros.json";

		for (Macro m : macros) {
			//build macros into json for POST
			String body = "{\"macro\":" + m.toString() + "}";

			//create the HTTP request
			Request request = AsyncRequest.buildGhosteryRequest("POST", body, ghosteryZendeskAPI);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				System.out.println("Macro Uploaded: " + result.getStatusCode() + " " + result.getStatusText());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Macro not uploaded: %s", m.toString());
				future.cancel(true);
			}
		}
	}

	/**
	 * Deserialize Actions JSONArray to List<Map> using GSON, and
	 * remove unnecessary properties
	 * @param json
	 * @return
	 */
	private static List<Map<String, Object>> buildActionList(JSONArray json) {
		Gson gson = new Gson();
		Type listType = new TypeToken<List<Map>>(){}.getType();
		List<Map<String, Object>> output = gson.fromJson(json.toString(), listType);

		//Check for and remove group_id, current_tags keys
		Iterator<Map<String, Object>> i = output.iterator();
		while (i.hasNext()) {
			Map<String, Object> child = i.next();
			String value = child.get("field").toString();
			if(Objects.equals(value, "group_id") || Objects.equals(value, "current_tags")) {
				i.remove();
			}
		}

		return output;
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	private void setTitle(String title) {
		this.title = title;
	}

	private void setDescription(String description) {
		this.description = description;
	}

	private void setActive(Boolean active) {
		this.active = active;
	}

	private void setActions(List<Map<String, Object>> actions) {
		this.actions = actions;
	}
}
