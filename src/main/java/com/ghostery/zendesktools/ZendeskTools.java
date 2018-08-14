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

	public ZendeskTools(String target) throws ExecutionException, InterruptedException {
		System.out.println("Running API target: " + target);
		switch (target) {
			case "articles" :
				//GET and POST all categories, sections and articles
				ArrayList<Article> articles = articleController.getArticles();
				articleController.postArticles(articles);
				break;
			case "macros" :
				//GET and POST all macros
				ArrayList<Macro> macros = macroController.getMacros();
				macroController.postMacros(macros);
				break;
			case "ticket" :
				//GET and POST a single ticket along with users and comments
				Ticket ticket = ticketController.getTicket(12697); //@TODO add ticketID to args
				ticketController.postTicket(ticket);
				break;
			case "tickets" :
				//Iterate over all tickets and POST in batches of 100, along with users and comments
				ticketController.getTickets();
				break;
			case "update_tickets" :
				//Update existing tickets
				TicketController.getNewTickets();
				break;
			case "update_author" :
				//Bulk change all article authors to Ghostery Support
				articleController.updateArtcleAuthor();
				break;
			default :
				break;
		}
	}

	/**
	 * Main method
	 * @param args      Zendesk API target
	 */
	public static void main(String[] args) {
		try {
			if (args.length != 0) {
				new ZendeskTools(args[0]);
			} else {
				System.out.println("Please specify an API to target");
			}
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
