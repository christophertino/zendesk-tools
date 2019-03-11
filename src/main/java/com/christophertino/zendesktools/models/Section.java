package com.christophertino.zendesktools.models;

import com.christophertino.zendesktools.interfaces.AsyncRequest;
import com.google.gson.Gson;

import java.util.HashMap;

/**
 * Zendesk Tools
 *
 * @author Christopher Tino
 * @since 1.0
 */
public class Section implements AsyncRequest {
	public static HashMap<Integer, Long> sectionIDs = new HashMap<>();

	private String name;
	private String description;
	private transient Integer legacyId; //ignore this when serializing
	private transient Long category_id; //ignore this when serializing

	public Section(String name, String description, Integer legacyId, Long category_id) {
		this.name = name;
		this.description = description;
		this.legacyId = legacyId;
		this.category_id = category_id;
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public Long getCategory_id() {
		return category_id;
	}

	public Integer getLegacyId() {
		return legacyId;
	}
}
