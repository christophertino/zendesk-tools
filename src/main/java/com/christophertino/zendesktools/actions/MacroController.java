package com.christophertino.zendesktools.actions;

import com.christophertino.zendesktools.models.Macro;
import com.christophertino.zendesktools.interfaces.AsyncRequest;
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

import static com.christophertino.zendesktools.interfaces.Constants.CURRENT_API_URL;
import static com.christophertino.zendesktools.interfaces.Constants.LEGACY_API_URL;

/**
 * Macro Controller
 *
 * @author Christopher Tino
 * @since 1.0
 */
public class MacroController {
	/**
	 * Retrieve a list of macros from Zendesk
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public ArrayList<Macro> getMacros() throws ExecutionException, InterruptedException {
		System.out.println("GETTING MACROS...");
		String legacyURL = LEGACY_API_URL + "/macros.json";

		//create the HTTP request
		Request request = AsyncRequest.buildLegacyRequest(legacyURL);
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
	private ArrayList<Macro> buildMacros(JSONArray macros) {
		ArrayList<Macro> output = new ArrayList<>();
		for (int i = 0; i < macros.length(); i++) {
			JSONObject macro = macros.getJSONObject(i);
			Macro m = new Macro(
					macro.getString("title"),
					macro.optString("description", null),
					macro.getBoolean("active"),
					buildActionList(macro.getJSONArray("actions"))
			);
			output.add(m);
		}
		return output;
	}

	/**
	 * POST an ArrayList of Macros to Zendesk, one-by-one
	 * @param macros
	 * @return
	 */
	public void postMacros(ArrayList<Macro> macros) {
		System.out.println("POSTING MACROS...");

		String currentURL = CURRENT_API_URL + "/macros.json";

		for (Macro m : macros) {
			//build macros into json for POST
			String body = "{\"macro\":" + m.toString() + "}";

			//create the HTTP request
			Request request = AsyncRequest.buildCurrentUpdateRequest("POST", body, currentURL);
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
	private List<Map<String, Object>> buildActionList(JSONArray json) {
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
}
