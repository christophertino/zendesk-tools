package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import com.google.gson.Gson;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
public class Article implements AsyncRequest {

	private String title;
	private String body;
	private Boolean comments_disabled;
	private transient Long section_id;

	public Article() {}

	/**
	 * Retrieve a batch of 100 articles along with associated categories and sections
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected static ArrayList<Article> getArticles() throws ExecutionException, InterruptedException {
		System.out.println("GETTING ARTICLES...");
		String evidonZendeskAPI = "https://ghostery.zendesk.com/api/v2/help_center/en-us/articles.json?include=categories,sections&per_page=100";

		//create the HTTP request
		Request request = AsyncRequest.buildEvidonRequest(evidonZendeskAPI);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract articles, sections and categories
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONArray responseCategoryArray = responseObject.getJSONArray("categories");
		JSONArray responseSectionArray = responseObject.getJSONArray("sections");
		JSONArray responseArticleArray = responseObject.getJSONArray("articles");

		//build new Categories and post to Zendesk
		ArrayList<Category> categoryList = Category.buildCategories(responseCategoryArray);
		Category.postCategories(categoryList);

		//build new Sections and post to Zendesk
		ArrayList<Section> sectionList = Section.buildSections(responseSectionArray);
		Section.postSections(sectionList);

		ArrayList<Article> articles = buildArticles(responseArticleArray);
		System.out.println("ARTICLES: " + articles.toString());

		return articles;
	}

	/**
	 * POST an ArrayList of Articles to Zendesk, one-by-one,
	 * @param articles  articles, sections or categories
	 */
	protected static void postArticles(ArrayList<Article> articles) {
		System.out.println("POSTING ARTICLES...");

		for (Article a : articles) {
			//build articles into json for POST
			String body = "{\"article\":" + a.toString() + "}";

			String ghosteryZendeskAPI = "https://ghosterysupport.zendesk.com/api/v2/help_center/en-us/sections/" + a.section_id + "articles.json";

			//create the HTTP request
			Request request = AsyncRequest.buildGhosteryRequest("POST", body, ghosteryZendeskAPI);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				System.out.println("Article Uploaded: " + result.getStatusCode() + " " + result.getStatusText());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Article not uploaded: %s", a.toString());
				future.cancel(true);
			}
		}
	}

	/**
	 * Factory function to generate Articles from JSONArray
	 * @param articles
	 */
	private static ArrayList<Article> buildArticles(JSONArray articles) {
		ArrayList<Article> output = new ArrayList<>();
		for (int i = 0; i < articles.length(); i++) {
			JSONObject article = articles.getJSONObject(i);
			Article a = new Article();

			a.setTitle(article.getString("title"));
			a.setBody(article.getString("body"));
			a.setSection_id(Section.sectionIDs.get(article.getInt("section_id"))); //get new section_id from map
			a.setComments_disabled(true);

			output.add(a);
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

	private void setBody(String body) {
		this.body = body;
	}

	private void setSection_id(Long section_id) {
		this.section_id = section_id;
	}

	private void setComments_disabled(Boolean comments_disabled) {
		this.comments_disabled = comments_disabled;
	}
}
