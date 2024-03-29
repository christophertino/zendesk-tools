package com.christophertino.zendesktools.models;

import com.christophertino.zendesktools.interfaces.AsyncRequest;
import com.google.gson.Gson;

/**
 * Zendesk Tools
 *
 * @author Christopher Tino
 * @since 1.0
 */
public class Article implements AsyncRequest {
	private String title;
	private String body;
	private Boolean comments_disabled;
	private transient Long section_id;

	public Article(String title, String body, Boolean comments_disabled, Long section_id) {
		this.title = title;
		this.body = body;
		this.comments_disabled = comments_disabled;
		this.section_id = section_id;
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public Long getSection_id() {
		return section_id;
	}
}
