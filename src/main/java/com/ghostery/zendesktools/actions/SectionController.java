package com.ghostery.zendesktools.actions;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.ghostery.zendesktools.models.Category;
import com.ghostery.zendesktools.models.Section;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ghostery.zendesktools.interfaces.Constants.CURRENT_API_URL;

/**
 * Section Controller
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */
public class SectionController {
	/**
	 * Factory function to generate Sections from JSONArray
	 * @param sections
	 */
	public static ArrayList<Section> buildSections(JSONArray sections) {
		ArrayList<Section> output = new ArrayList<>();
		for (int i = 0; i < sections.length(); i++) {
			JSONObject section = sections.getJSONObject(i);
			Section s = new Section(
					section.getString("name"),
					section.getString("description"),
					section.getInt("id"), //old section ID
					Category.categoryIDs.get(section.getInt("category_id")) //get new category_id from map
			);
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

			String currentURL = CURRENT_API_URL + "/help_center/en-us/categories/" + s.getCategory_id() + "sections.json";

			//create the HTTP request
			Request request = AsyncRequest.buildCurrentUpdateRequest("POST", body, currentURL);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				JSONObject responseObject = new JSONObject(result.getResponseBody());
				if (result.getStatusCode() <= 201) { //200 update, 201 create
					Long newSectionId = responseObject.getJSONObject("section").getLong("id");
					//map old sectionID to new sectionID
					System.out.println("Mapping old sectionIDs: " + s.getLegacyId() + " to new sectionID: " + newSectionId);
					Section.sectionIDs.put(s.getLegacyId(), newSectionId);
					System.out.println("Post Section:" + result.getStatusCode() + " " + result.getStatusText() + " ID: " + Section.sectionIDs.get(s.getLegacyId()));
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
}
