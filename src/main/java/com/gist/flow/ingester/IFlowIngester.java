package com.gist.flow.ingester;

import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;

import java.util.Set;

/**
 * IFlowIngester
 * @author Giuseppe Vincenzi
 *
 */
public interface IFlowIngester<T extends FlowResource> {
	void ingest(Set<T> contents) throws FlowException;
}
