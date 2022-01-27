package com.ioevent.starter.annotations;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * IOEvent Response annotation allows us to simplify the IOEvent response ,
 **/
@Data
@Builder
@AllArgsConstructor
public class IOResponse<T> {

	private String string;
	private T body;
	private Map<String, Object> headers = new HashMap<>();

	public IOResponse(String string, T body) {
		this.body = body;
		this.string = string;

	}
	public IOResponse(T body,Map<String, Object> headers) {
		this.body = body;
		this.headers = headers;

	}
}
