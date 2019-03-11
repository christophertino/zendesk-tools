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

public class User implements AsyncRequest {
	public static HashMap<Integer, Long> userIDs = new HashMap<>();

	private String name;
	private String email;
	private String role;
	private Boolean verified;
	private transient Integer legacyId; //ignore this when serializing

	public User(String name, String email, String role, Boolean verified, Integer legacyId) {
		this.name = name;
		this.email = email;
		this.role = role;
		this.verified = verified;
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
