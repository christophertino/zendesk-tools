package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import static com.ghostery.zendeskmigration.interfaces.Constants.*;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */

public class ZendeskMigration implements AsyncRequest {

	private final String evidonCreds = Base64.getEncoder().encodeToString((EVIDON_USER + "/token:" + EVIDON_TOKEN).getBytes(StandardCharsets.UTF_8));
	private final String ghosteryCreds = Base64.getEncoder().encodeToString((GHOSTERY_USER + "/token:" + GHOSTERY_TOKEN).getBytes(StandardCharsets.UTF_8));
	private HashMap<Integer, Long> userIDs = new HashMap<>();

	private ZendeskMigration() throws ExecutionException, InterruptedException {
//		HelpCenter hc = new HelpCenter();
//		hc.getHelpCenterContent("categories");
//		hc.getHelpCenterContent("sections");
//		hc.getHelpCenterContent("articles");

		//this.getTicket(10629);
		this.getTickets();
	}

	public static void main(String[] args) {
		try {
			new ZendeskMigration();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
