package com.ghostery.zendeskmigration;

import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ghostery.zendeskmigration.Constants.*;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */
public class HelpCenter implements AsyncRequest {

	private final String evidonCreds;
	private final String ghosteryCreds;
	private HashMap<Integer, Long> sectionIDs = new HashMap<>();

	public HelpCenter() {
		this.evidonCreds = Base64.getEncoder().encodeToString((EVIDON_USER + "/token:" + EVIDON_TOKEN).getBytes(StandardCharsets.UTF_8));
		this.ghosteryCreds = Base64.getEncoder().encodeToString((GHOSTERY_USER + "/token:" + GHOSTERY_TOKEN).getBytes(StandardCharsets.UTF_8));
		this.mapSectionIDs();
	}

	/**
	 * Fetch from article, section or category API
	 * @param type  articles, sections or categories
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public void getHelpCenterContent(String type) throws ExecutionException, InterruptedException {
		System.out.println("FETCHING " + type.toUpperCase() + "...");
		String evidonHelpCenterURL = "https://ghostery.zendesk.com/api/v2/help_center/en-us/";

		RequestBuilder builder = new RequestBuilder("GET");
		Request request = builder.setUrl(evidonHelpCenterURL + type + ".json?per_page=100") //&page=2
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + this.evidonCreds)
				.build();

		Future<Response> future = this.doAsyncRequest(request);
		//block execution until future resolves
		Response result = future.get();
		System.out.println("Future done? " + future.isDone());

		//Convert to JSON Object and extract data
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONArray responseArray = responseObject.getJSONArray(type);

		JSONArray content = new JSONArray();
		for (int i = 0; i < responseArray.length(); i++) {
			JSONObject obj = new JSONObject();
			if (type.equals("articles")) {
				String title = responseArray.getJSONObject(i).getString("title");
				String body = responseArray.getJSONObject(i).getString("body");
				Integer section_id = responseArray.getJSONObject(i).getInt("section_id");
				obj.put("title", title);
				obj.put("body", body);
				obj.put("section_id", section_id);
				obj.put("comments_disabled", true);
			} else {
				String name = responseArray.getJSONObject(i).getString("name");
				String description = responseArray.getJSONObject(i).getString("description");
				obj.put("name", name);
				obj.put("description", description);
			}
			content.put(obj);
		}

		System.out.printf( "JSON: %s", content.toString(2) );

		this.postHelpCenterContent(type, content);
	}

	/**
	 * Post to article, section or category API
	 * @param type  articles, sections or categories
	 * @param content  from fetch
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void postHelpCenterContent(String type, JSONArray content) throws ExecutionException, InterruptedException {
		System.out.println("BUILDING " + type.toUpperCase() + "...");

		String ghosteryHelpCenterURL = "https://ghosterysupport.zendesk.com/api/v2/help_center/en-us/";
		String jsonType = (type.equals("sections")) ? "section" : (type.equals("articles")) ? "article" : "category";

		for (int i = 0; i < content.length(); i++) {
			String body = "{\"" + jsonType + "\":" + content.get(i).toString() + "}";
			//Map old section IDs to new for articles. Sections all go under the default category.
			String urlType = (type.equals("sections")) ? "categories/115000106314/" + type :
					(type.equals("articles")) ? "sections/"+ this.sectionIDs.get(content.getJSONObject(i).getInt("section_id"))+ "/" + type : type;

			RequestBuilder builder = new RequestBuilder("POST");
			Request request = builder.setUrl(ghosteryHelpCenterURL + urlType + ".json")
					.addHeader("Content-Type", "application/json")
					.addHeader("Accept", "application/json")
					.addHeader("Authorization", "Basic " + this.ghosteryCreds)
					.setBody(body)
					.build();

			Future<Response> future = this.doAsyncRequest(request);
			Response result;
			try {
				result = future.get(15, TimeUnit.SECONDS);
				//TODO: Save section_id and category_id to HashMap
				System.out.println(result.getStatusCode() + " " + result.getStatusText());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Article not uploaded: %s", content.get(i).toString());
				future.cancel(true);
			}
		}
	}

	/**
	 * Map old / new section IDs
	 */
	private void mapSectionIDs() {
		//API won't query sections without articles, so we have to map them manually (old, new)
		this.sectionIDs.put(201819186, 115000206753L);
		this.sectionIDs.put(201089089, 115000211754L);
		this.sectionIDs.put(201164365, 115000211774L);
		this.sectionIDs.put(202026926, 115000206773L);
		this.sectionIDs.put(202090123, 115000211794L);
		this.sectionIDs.put(202026946, 115000211814L);
		this.sectionIDs.put(201809893, 115000211834L);
		this.sectionIDs.put(201809913, 115000206793L);
		this.sectionIDs.put(202089523, 115000206813L);
		this.sectionIDs.put(201819206, 115000206833L);
		this.sectionIDs.put(202089263, 115000211854L);
		this.sectionIDs.put(202028303, 115000211874L);
		this.sectionIDs.put(201819176, 115000206853L);
		this.sectionIDs.put(202027006, 115000211894L);
		this.sectionIDs.put(201809903, 115000211914L);
		this.sectionIDs.put(202707966, 115000211934L);
		this.sectionIDs.put(202085046, 115000211954L);
		this.sectionIDs.put(201819196, 115000211974L);
		this.sectionIDs.put(202090483, 115000206873L);
		this.sectionIDs.put(201819226, 115000211994L);
		this.sectionIDs.put(201819216, 115000206893L);
	}
}
