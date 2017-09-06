package com.ghostery.zendeskmigration;

import com.ghostery.zendeskmigration.interfaces.AsyncRequest;

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
		//GET and POST all categories, sections and articles
//		ArrayList<Article> articles = Article.getArticles();
//		Article.postArticles(articles);

		//GET and POST all macros
//		ArrayList<Macro> macros = Macro.getMacros();
//		Macro.postMacros(macros);

		//GET and POST a single ticket along with users and comments
//		Ticket ticket = Ticket.getTicket(12697);
//		Ticket.postTicket(ticket);

		//Iterate over all tickets and POST in batches of 100, along with users and comments
//		Ticket.getTickets();

		//Update existing tickets
		Ticket.getNewTickets();
	}

	public static void main(String[] args) {
		try {
			new ZendeskMigration();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
