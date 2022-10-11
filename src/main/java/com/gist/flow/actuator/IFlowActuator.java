package com.gist.flow.actuator;

import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;

import java.util.Set;

/**
 * IFlowActuator
 * @author Giuseppe Vincenzi
 *
 */
public interface IFlowActuator<T extends FlowResource> {
	void doAction(Set<T> resources) throws FlowException;
}
