package com.ghostery.zendesktools.models;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.google.gson.Gson;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.ghostery.zendesktools.interfaces.Constants.CURRENT_API_URL;

/**
 * Ghostery Zendesk Tools
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
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
