package com.ghostery.zendeskmigration.interfaces;

import org.asynchttpclient.*;

import java.util.concurrent.Future;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */

public interface AsyncRequest {
	/**
	 * Utility method to execute AsyncHttpClient
	 * @param request
	 * @return
	 */
	default Future<Response> doAsyncRequest(Request request) {
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
}
