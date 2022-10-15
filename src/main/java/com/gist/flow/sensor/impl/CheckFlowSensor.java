package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.impl.CheckFlowActuator;
import com.gist.flow.actuator.impl.SoffBlogFlowActuator;
import com.gist.flow.exception.FlowException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@Service
@Profile("checkflow")
public class CheckFlowSensor extends RssFlowSensor{
	@Autowired
	public CheckFlowSensor(@Autowired CheckFlowActuator checkFlowActuator) {
		super(checkFlowActuator);
	}

	@Override
	public Calendar getLastChangeDate() {
		Calendar lastChangeDate = Calendar.getInstance();
		lastChangeDate.roll(Calendar.DAY_OF_YEAR, -2);
		return lastChangeDate;
	}
}
