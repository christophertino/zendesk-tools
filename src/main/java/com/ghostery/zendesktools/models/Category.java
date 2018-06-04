package com.ghostery.zendesktools.models;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.google.gson.Gson;
import java.util.HashMap;

/**
 * Ghostery Zendesk Tools
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */
public class Category implements AsyncRequest {
	public static HashMap<Integer, Long> categoryIDs = new HashMap<>();

	private String name;
	private String description;
	private transient Integer legacyId; //ignore this when serializing

	public Category(String name, String description, Integer legacyId){
		this.name = name;
		this.description = description;
		this.legacyId = legacyId;
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this);
	}

	public Integer getLegacyId() {
		return legacyId;
	}
}
