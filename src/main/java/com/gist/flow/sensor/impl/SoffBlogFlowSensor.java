package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.impl.SoffBlogFlowActuator;
import com.gist.flow.exception.FlowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@Service
@Profile("soffblog")
public class SoffBlogFlowSensor extends RssFlowSensor{
	@Autowired
	public SoffBlogFlowSensor(@Autowired SoffBlogFlowActuator soffBlogFlowActuator) {
		super(soffBlogFlowActuator);
	}

	@Override
	public Calendar getLastChangeDate() {
		try {
			return ((SoffBlogFlowActuator)getRssFlowActuator()).getLastPostDate();
		} catch (FlowException e) {
			return super.getLastChangeDate();
		}
	}
}
