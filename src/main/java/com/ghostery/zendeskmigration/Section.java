package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import com.google.gson.Gson;
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
public class Section implements AsyncRequest {

	protected static HashMap<Integer, Long> sectionIDs = new HashMap<>();
	private String name;
	private String description;
	private transient Integer legacyId; //ignore this when serializing
	private transient Long category_id; //ignore this when serializing

	private Section(){}

	/**
	 * Factory function to generate Sections from JSONArray
	 * @param sections
	 */
	protected static ArrayList<Section> buildSections(JSONArray sections) {
		ArrayList<Section> output = new ArrayList<>();
		for (int i = 0; i < sections.length(); i++) {
			JSONObject section = sections.getJSONObject(i);
			Section s = new Section();

			s.setName(section.getString("name"));
			s.setDescription(section.getString("description"));
			s.setLegacyId(section.getInt("id")); //old section ID
			s.setCategory_id(Category.categoryIDs.get(section.getInt("category_id"))); //get new category_id from map

			output.add(s);
		}
		return output;
	}

	/**
	 * POST an ArrayList of Sections to Zendesk, one-by-one,
	 * mapping old/new sectionIDs along the way
	 * @param sections
	 * @return
	 */
	protected static void postSections(ArrayList<Section> sections) {
		System.out.println("POSTING SECTIONS...");

		for (Section s : sections) {
			//build sections into json for POST
			String body = "{\"section\":" + s.toString() + "}";

			String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/help_center/en-us/categories/" + s.category_id + "sections.json";

			//create the HTTP request
			Request request = AsyncRequest.buildGhosteryRequest("POST", body, ghosteryZendeskAPI);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				JSONObject responseObject = new JSONObject(result.getResponseBody());
				if (result.getStatusCode() <= 201) { //200 update, 201 create
					Long newSectionId = responseObject.getJSONObject("section").getLong("id");
					//map old sectionID to new sectionID
					System.out.println("Mapping old sectionIDs: " + s.legacyId + " to new sectionID: " + newSectionId);
					sectionIDs.put(s.legacyId, newSectionId);
					System.out.println("Post Section:" + result.getStatusCode() + " " + result.getStatusText() + " ID: " + sectionIDs.get(s.legacyId));
				} else {
					System.out.println("Post Section error " + result.getStatusCode() + " " + result.getStatusText());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Section not uploaded: %s", s.toString());
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

	private void setDescription(String description) {
		this.description = description;
	}

	private void setLegacyId(Integer legacyId) {
		this.legacyId = legacyId;
	}

	public void setCategory_id(Long category_id) {
		this.category_id = category_id;
	}
}
