package com.christophertino.zendesktools.models;

import com.christophertino.zendesktools.interfaces.AsyncRequest;
import com.google.gson.Gson;
import java.util.ArrayList;

/**
 * Zendesk Tools
 *
 * @author Christopher Tino
 * @since 1.0
 */

public class Ticket implements AsyncRequest {
	private Integer id;
	private String subject;
	private Long requester_id;
	private Long assignee_id;
	private String status;
	private String created_at;
	private String updated_at;
	private Long[] sharing_agreement_ids = new Long[1];
	private String comment;
	private ArrayList<Comment> comments;
	private transient Integer legacyID;

	public Ticket(Integer id, String status, Long sharing_agreement_id) {
		this.id = id;
		this.status = status;
		this.sharing_agreement_ids[0] = sharing_agreement_id;
	}

	public Ticket(String subject, Long requester_id, Long assignee_id, String status, String created_at, String updated_at) {
		this.subject = subject;
		this.requester_id = requester_id;
		this.assignee_id = assignee_id;
		this.status = status;
		this.created_at = created_at;
		this.updated_at = updated_at;
	}

	public Ticket(String subject, Long requester_id, Long assignee_id, String status, String created_at, String updated_at, Integer id) {
		this.subject = subject;
		this.requester_id = requester_id;
		this.assignee_id = assignee_id;
		this.status = status;
		this.created_at = created_at;
		this.updated_at = updated_at;
		this.id = id;
	}

	@Override
	public String toString(){
		Gson gson = new Gson();
		return gson.toJson(this).replace("is_public", "public"); //for Comments
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setComments(ArrayList<Comment> comments) {
		this.comments = comments;
	}

	public Integer getLegacyID() {
		return legacyID;
	}
}
