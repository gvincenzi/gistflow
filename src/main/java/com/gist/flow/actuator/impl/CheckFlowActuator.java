package com.gist.flow.actuator.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.exception.FlowException;
import com.gist.flow.ingester.impl.TelegramBotIngester;
import com.gist.flow.model.entity.FlowResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@Profile("checkflow")
public class CheckFlowActuator implements IFlowActuator<FlowResource> {

	@Override
	public void doAction(Set<FlowResource> resources) throws FlowException {
		log.info(String.format("CheckFlowActuator doAction has been called with [%d] resources",resources.size()));
		for(FlowResource resource : resources){
			log.info(resource.getName());
		}
		log.info(String.format("CheckFlowActuator doAction has been successfully with [%d] resources",resources.size()));
	}

}
