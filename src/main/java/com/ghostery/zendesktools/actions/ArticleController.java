package com.ghostery.zendesktools.actions;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.ghostery.zendesktools.models.Article;
import com.ghostery.zendesktools.models.Category;
import com.ghostery.zendesktools.models.Section;
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
import static com.ghostery.zendesktools.interfaces.Constants.NEW_AUTHOR_ID;

/**
 * Article Controller
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */
public class ArticleController {
	/**
	 * Retrieve a batch of 100 articles along with associated categories and sections
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public ArrayList<Article> getArticles() throws ExecutionException, InterruptedException {
		System.out.println("GETTING ARTICLES...");
		String legacyURL = LEGACY_API_URL + "/help_center/en-us/articles.json?include=categories,sections&per_page=100";

		//create the HTTP request
		Request request = AsyncRequest.buildLegacyRequest(legacyURL);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract articles, sections and categories
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONArray responseCategoryArray = responseObject.getJSONArray("categories");
		JSONArray responseSectionArray = responseObject.getJSONArray("sections");
		JSONArray responseArticleArray = responseObject.getJSONArray("articles");

		//build new Categories and post to Zendesk
		ArrayList<Category> categoryList = CategoryController.buildCategories(responseCategoryArray);
		CategoryController.postCategories(categoryList);

		//build new Sections and post to Zendesk
		ArrayList<Section> sectionList = SectionController.buildSections(responseSectionArray);
		SectionController.postSections(sectionList);

		ArrayList<Article> articles = buildArticles(responseArticleArray);
		System.out.println("ARTICLES: " + articles.toString());

		return articles;
	}

	/**
	 * Factory function to generate Articles from JSONArray
	 * @param articles
	 */
	private ArrayList<Article> buildArticles(JSONArray articles) {
		ArrayList<Article> output = new ArrayList<>();
		for (int i = 0; i < articles.length(); i++) {
			JSONObject article = articles.getJSONObject(i);
			Article a = new Article(
					article.getString("title"),
					article.getString("body"),
					true,
					Section.sectionIDs.get(article.getInt("section_id")) //get new section_id from map
			);

			output.add(a);
		}
		return output;
	}

	/**
	 * POST an ArrayList of Articles to Zendesk, one-by-one,
	 * @param articles  articles, sections or categories
	 */
	public void postArticles(ArrayList<Article> articles) {
		System.out.println("POSTING ARTICLES...");

		for (Article a : articles) {
			//build articles into json for POST
			String body = "{\"article\":" + a.toString() + "}";

			String currentURL = CURRENT_API_URL + "/help_center/en-us/sections/" + a.getSection_id() + "articles.json";

			//create the HTTP request
			Request request = AsyncRequest.buildCurrentUpdateRequest("POST", body, currentURL);
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
	 * GET all Articles, update author_id and PUT back
	 */
	public void updateArtcleAuthor() throws ExecutionException, InterruptedException {
		System.out.println("GETTING ALL ARTICLES...");
		String currentURL = CURRENT_API_URL + "/help_center/en-us/articles.json?per_page=100";

		//create the HTTP request
		Request request = AsyncRequest.buildCurrentGetRequest(currentURL);
		Future<Response> future = AsyncRequest.doAsyncRequest(request);
		Response result = future.get();

		//Convert response to JSON Object and extract articles
		JSONObject responseObject = new JSONObject(result.getResponseBody());
		JSONArray responseArticleArray = responseObject.getJSONArray("articles");

		//update each article
		for (int i = 0; i < responseArticleArray.length(); i++) {
			JSONObject article = responseArticleArray.getJSONObject(i);
			Long articleID = article.getLong("id");

			//build article into json
			String body = "{\"article\": {\"author_id\":" + NEW_AUTHOR_ID + "}}";

			String updateURL = CURRENT_API_URL + "/help_center/en-us/articles/" + articleID + ".json";

			//create the HTTP request
			Request update = AsyncRequest.buildCurrentUpdateRequest("PUT", body, updateURL);
			Future<Response> futureUpdate = AsyncRequest.doAsyncRequest(update);
			Response resultUpdate;

			try {
				resultUpdate = futureUpdate.get(15, TimeUnit.SECONDS);
				System.out.println("Article author_id updated: " + resultUpdate.getStatusCode() + " " + resultUpdate.getStatusText());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Article author_id not updated: %s", articleID);
				future.cancel(true);
			}
		}
	}
}
