package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.actuator.impl.SoffBlogFlowActuator;
import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Set;

@Slf4j
@Service
@Profile({"soffblog"})
public class SoffBlogFlowSensor extends RssFlowSensor{
	@Autowired
	public SoffBlogFlowSensor(@Autowired Set<IFlowActuator<FlowResource>> flowActuators) {
		super(flowActuators);
		for (IFlowActuator<FlowResource> flowActuator : flowActuators){
			if(flowActuator.getClass() != SoffBlogFlowActuator.class){
				getRssFlowActuators().remove(flowActuator);
			} else {
				log.info(String.format("SoffBlogFlowSensor has been created with an Actuator of type [%s]",
						flowActuator.getClass().getName()));
			}
		}
	}

	@Override
	public Calendar getLastChangeDate() {
		try {
			SoffBlogFlowActuator soffBlogFlowActuator = null;
			for (IFlowActuator<FlowResource> flowActuator : getRssFlowActuators()){
				if(flowActuator.getClass() == SoffBlogFlowActuator.class){
					soffBlogFlowActuator = (SoffBlogFlowActuator)flowActuator;
					break;
				}
			}
			return soffBlogFlowActuator != null ? soffBlogFlowActuator.getLastPostDate() : super.getLastChangeDate();
		} catch (FlowException e) {
			return super.getLastChangeDate();
		}
	}
}
