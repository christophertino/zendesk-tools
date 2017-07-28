package com.ghostery.zendeskmigration.interfaces;

import org.asynchttpclient.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Future;

import static com.ghostery.zendeskmigration.interfaces.Constants.*;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */

public interface AsyncRequest {

	String evidonCreds = Base64.getEncoder().encodeToString((EVIDON_USER + "/token:" + EVIDON_TOKEN).getBytes(StandardCharsets.UTF_8));
	String ghosteryCreds = Base64.getEncoder().encodeToString((GHOSTERY_USER + "/token:" + GHOSTERY_TOKEN).getBytes(StandardCharsets.UTF_8));

	/**
	 * Utility method to execute AsyncHttpClient
	 * @param request
	 * @return
	 */
	static Future<Response> doAsyncRequest(Request request) {
		AsyncHttpClient client = new DefaultAsyncHttpClient();
		//returns Future<response>
		return client.executeRequest(request, new AsyncCompletionHandler<Response>() {
			@Override
			public Response onCompleted(Response response) throws Exception{
				//System.out.println(response.getStatusCode());
				//System.out.println(response.getResponseBody());
				return response;
			}

			@Override
			public void onThrowable(Throwable t){
				t.printStackTrace();
			}
		});
	}

	static Request buildEvidonRequest(String url) {
		RequestBuilder builder = new RequestBuilder("GET");
		return builder.setUrl(url)
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + evidonCreds)
				.build();
	}

	static Request buildGhosteryRequest(String body, String url) {
		RequestBuilder builder = new RequestBuilder("POST");
		return builder.setUrl(url)
				.addHeader("Content-Type", "application/json")
				.addHeader("Accept", "application/json")
				.addHeader("Authorization", "Basic " + ghosteryCreds)
				.setBody(body)
				.build();
	}

}
