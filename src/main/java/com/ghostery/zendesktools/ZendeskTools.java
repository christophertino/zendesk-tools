package com.ghostery.zendesktools;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.ghostery.zendesktools.actions.ArticleController;
import com.ghostery.zendesktools.actions.MacroController;
import com.ghostery.zendesktools.actions.TicketController;
import com.ghostery.zendesktools.interfaces.AsyncRequest;
import com.ghostery.zendesktools.models.Article;
import com.ghostery.zendesktools.models.Macro;
import com.ghostery.zendesktools.models.Ticket;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Zendesk Tools
 *
 * @author Ghostery Engineering
 *
 * Copyright 2018 Ghostery, Inc. All rights reserved.
 */

public class ZendeskTools implements AsyncRequest, RequestHandler<Map<String,Object>, String> {
	private ArticleController articleController = new ArticleController();
	private MacroController macroController = new MacroController();
	private TicketController ticketController = new TicketController();

	public ZendeskTools() throws ExecutionException, InterruptedException {
		//GET and POST all categories, sections and articles
		ArrayList<Article> articles = articleController.getArticles();
		articleController.postArticles(articles);

		//GET and POST all macros

		ArrayList<Macro> macros = macroController.getMacros();
		macroController.postMacros(macros);

		//GET and POST a single ticket along with users and comments

		Ticket ticket = ticketController.getTicket(12697);
		ticketController.postTicket(ticket);

		//Iterate over all tickets and POST in batches of 100, along with users and comments
		ticketController.getTickets();

		//Update existing tickets
		TicketController.getNewTickets();

		//Bulk change all article authors to Ghostery Support
		articleController.updateArtcleAuthor();
	}

	public static void main(String[] args) {
		try {
			new ZendeskTools();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Entry point for AWS Lambda execution
	 * @param input     set this to a Map<> to handle JSON input
	 * @param context
	 * @return
	 */
	@Override
	public String handleRequest(Map<String,Object> input, Context context) {
		context.getLogger().log("AWS Lambda Initialized...");
		try {
			TicketController.getNewTickets();
		} catch (ExecutionException | InterruptedException e) {
			e.printStackTrace();
		}
		return "Lambda Completed.";
	}
}
