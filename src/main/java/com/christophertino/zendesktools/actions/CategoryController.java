package com.christophertino.zendesktools.actions;

import com.christophertino.zendesktools.models.Category;
import com.christophertino.zendesktools.interfaces.AsyncRequest;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.christophertino.zendesktools.interfaces.Constants.CURRENT_API_URL;

/**
 * Category Controller
 *
 * @author Christopher Tino
 * @since 1.0
 */
public class CategoryController {
	/**
	 * Factory function to generate Categories from JSONArray
	 * @param categories
	 */
	public static ArrayList<Category> buildCategories(JSONArray categories) {
		ArrayList<Category> output = new ArrayList<>();
		for (int i = 0; i < categories.length(); i++) {
			JSONObject category = categories.getJSONObject(i);
			Category c = new Category(
					category.getString("name"),
					category.getString("description"),
					category.getInt("id")
			);
			output.add(c);
		}
		return output;
	}

	/**
	 * POST an ArrayList of Categories to Zendesk, one-by-one,
	 * mapping old/new categoryIDs along the way
	 * @param categories
	 * @return
	 */
	public static void postCategories(ArrayList<Category> categories) {
		System.out.println("POSTING CATEGORIES...");

		String currentURL = CURRENT_API_URL + "/help_center/en-us/categories.json";

		for (Category c : categories) {
			//build categories into json for POST
			String body = "{\"category\":" + c.toString() + "}";

			//create the HTTP request
			Request request = AsyncRequest.buildCurrentUpdateRequest("POST", body, currentURL);
			Future<Response> future = AsyncRequest.doAsyncRequest(request);
			Response result;

			try {
				result = future.get(15, TimeUnit.SECONDS);
				JSONObject responseObject = new JSONObject(result.getResponseBody());
				if (result.getStatusCode() <= 201) { //200 update, 201 create
					Long newCategoryId = responseObject.getJSONObject("category").getLong("id");
					//map old categoryID to new categoryID
					System.out.println("Mapping old categoryID: " + c.getLegacyId() + " to new categoryID: " + newCategoryId);
					Category.categoryIDs.put(c.getLegacyId(), newCategoryId);
					System.out.println("Post Category:" + result.getStatusCode() + " " + result.getStatusText() + " ID: " + Category.categoryIDs.get(c.getLegacyId()));
				} else {
					System.out.println("Post Category error " + result.getStatusCode() + " " + result.getStatusText());
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Category not uploaded: %s", c.toString());
				future.cancel(true);
			}
		}
	}
}
