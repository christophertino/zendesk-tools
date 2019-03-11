package com.christophertino.zendesktools.interfaces;

import org.asynchttpclient.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Future;

import static com.christophertino.zendesktools.interfaces.Constants.*;

/**
 * Zendesk Tools
 *
 * @author Christopher Tino
 * @since 1.0
 */

public interface AsyncRequest {

	String legacyCreds = Base64.getEncoder().encodeToString((LEGACY_USER + "/token:" + LEGACY_TOKEN).getBytes(StandardCharsets.UTF_8));
	String currentCreds = Base64.getEncoder().encodeToString((CURRENT_USER + "/token:" + CURRENT_TOKEN).getBytes(StandardCharsets.UTF_8));

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

	/**
	 * Build GET requests against legacy Zendesk account
	 * @param url
	 * @return
	 */
	static Request buildLegacyRequest(String url) {
		RequestBuilder builder = new RequestBuilder("GET");
		return builder.setUrl(url)
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + legacyCreds)
				.build();
	}

	/**
	 * Build GET requests against current Zendesk account
	 * @param url
	 * @return
	 */
	static Request buildCurrentGetRequest(String url) {
		RequestBuilder builder = new RequestBuilder("GET");
		return builder.setUrl(url)
				.addHeader("Accept","application/json")
				.addHeader("Authorization", "Basic " + currentCreds)
				.build();
	}

	/**
	 * Build POST and PUT requests against current Zendesk account
	 * @param type     "GET", "POST" or "PUT"
	 * @param body
	 * @param url
	 * @return
	 */
	static Request buildCurrentUpdateRequest(String type, String body, String url) {
		RequestBuilder builder = new RequestBuilder(type);
		return builder.setUrl(url)
				.addHeader("Content-Type", "application/json")
				.addHeader("Accept", "application/json")
				.addHeader("Authorization", "Basic " + currentCreds)
				.setBody(body)
				.build();
	}

}
