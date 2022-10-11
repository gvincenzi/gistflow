package com.gist.flow.actuator.impl;

import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import lombok.extern.java.Log;
import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.ingester.impl.WordPressIngester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Set;

@Log
@Service
@Profile("soffblog")
public class SoffBlogFlowActuator implements IFlowActuator<FlowResource> {
	@Autowired
	private WordPressIngester ingester;
	
	@Override
	public void doAction(Set<FlowResource> resources) throws FlowException {
		log.info(String.format("SoffBlogFlowActuator doAction has been called with [%d] resources",resources.size()));
		ingester.ingest(resources);
		log.info(String.format("SoffBlogFlowActuator doAction has been successfully with [%d] resources",resources.size()));
	}
	
	public Calendar getLastPostDate() throws FlowException{
		return ingester.getLastPostDate();
	}

}
