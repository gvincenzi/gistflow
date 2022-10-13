package com.gist.flow.sensor;

import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;

import java.util.Calendar;
import java.util.Set;

/**
 * IFlowSensor
 * @author Giuseppe Vincenzi
 *
 */
public interface IFlowSensor<T extends FlowResource>{
	void onChange(Set<T> resources) throws FlowException;
	void startSensor();
	Calendar getLastChangeDate();
}
