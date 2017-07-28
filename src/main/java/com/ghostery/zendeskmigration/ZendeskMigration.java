package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

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

//		Ticket ticket = Ticket.getTicket(10629);
//		Ticket.postTicket(ticket);

		ArrayList<Ticket> tickets = Ticket.getTickets();
		Ticket.postManyTickets(tickets);
	}

	public static void main(String[] args) {
		try {
			new ZendeskMigration();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
