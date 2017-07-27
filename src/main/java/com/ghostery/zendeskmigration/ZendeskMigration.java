package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;
import java.util.concurrent.ExecutionException;
import com.ghostery.zendeskmigration.Tickets;

/**
 * Zendesk Migration
 *
 * @author Christopher Tino
 *
 * Copyright 2017 Ghostery, Inc. All rights reserved.
 * See https://www.ghostery.com/eula for license.
 */

public class ZendeskMigration implements AsyncRequest {

	private ZendeskMigration() throws ExecutionException, InterruptedException {
//		HelpCenter hc = new HelpCenter();
//		hc.getHelpCenterContent("categories");
//		hc.getHelpCenterContent("sections");
//		hc.getHelpCenterContent("articles");

		getTicket(10629);
		//this.getTickets();
	}

	public static void main(String[] args) {
		try {
			new ZendeskMigration();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
