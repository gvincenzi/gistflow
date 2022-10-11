package com.gist.flow.exception;

/**
 * FlowException
 * @author Giuseppe Vincenzi
 *
 */
public class FlowException extends Exception {

	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 3598519926879443947L;

	/**
	 * BYOFlowException Constructor
	 *
	 * @param e Exception
	 */
	public FlowException(Exception e) {
		super(e.getMessage());
	}

	/**
	 * BYOFlowException Constructor
	 *
	 * @param message String
	 */
	public FlowException(String message) {
		super(message);
	}

	/**
	 * BYOFlowException Constructor
	 *
	 * @param t Throwable
	 */
	public FlowException(Throwable t) {
		super(t);
	}
}
