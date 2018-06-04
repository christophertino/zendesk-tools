package com.ghostery.zendesktools.models;

import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.google.gson.Gson;
import java.util.*;

/**
 * Ghostery Zendesk Tools
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */

public class Macro implements AsyncRequest {
	private String title;
	private String description;
	private Boolean active;
	private List<Map<String, Object>> actions;

	public Macro(String title, String description, Boolean active, List<Map<String, Object>> actions) {
		this.title = title;
		this.description = description;
		this.active = active;
		this.actions = actions;
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this);
	}
}
