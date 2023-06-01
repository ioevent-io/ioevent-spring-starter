package com.ioevent.starter.annotations;

import lombok.Data;

@Data
public class ConditionalIOResponse<T> extends IOResponse<T> {

	/**
	 * attribute for conditional start event
	 */
	private boolean condition = true;
	/**
	 * create IOResponse with conditional Used mostly in conditional start event
	 * 
	 * @param key
	 * @param body
	 * @param conditional
	 */
	public ConditionalIOResponse(String key, T body, boolean conditional) {
		super(key,body);
		this.condition = conditional;

	}
}
