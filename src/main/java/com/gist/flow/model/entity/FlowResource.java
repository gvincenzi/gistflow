package com.gist.flow.model.entity;

import lombok.Data;

import java.util.Calendar;

@Data
public class FlowResource {
	private String name;
	private String description;
	private Calendar startDateOfValidity = Calendar.getInstance();
	private String recipientID;
}
