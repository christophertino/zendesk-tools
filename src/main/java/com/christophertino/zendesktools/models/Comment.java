package com.christophertino.zendesktools.models;

import com.christophertino.zendesktools.interfaces.AsyncRequest;
import com.google.gson.Gson;

/**
 * Zendesk Tools
 *
 * @author Christopher Tino
 * @since 1.0
 */

public class Comment implements AsyncRequest {
	private String body;
	private Long author_id;
	private String created_at;
	private Boolean is_public;

	public Comment(String body, Long author_id, String created_at, Boolean is_public) {
		this.body = body;
		this.author_id = author_id;
		this.created_at = created_at;
		this.is_public = is_public;
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this).replace("is_public", "public");
	}
}
