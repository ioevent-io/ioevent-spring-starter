package com.grizzlywave.grizzlywavestarter.model;

import java.util.HashMap;

/**
 * Class WaveResponse Contains an object and a hash map to give more information
 * about events after sending or receiving using GrizzlyWave annotations
 * 
 * @author Ahmed
 **/
public class WaveResponse<T> {

	/**
	 * object of the events
	 **/
	T object;
	/**
	 * HashMap to add more information about event
	 **/
	HashMap<String, Object> Headers;

	/**
	 * Constructor from super class
	 **/
	public WaveResponse() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * constructor using parameters
	 **/
	public WaveResponse(T object, HashMap<String, Object> Headers) {
		// TODO Auto-generated constructor stub
		this.object = object;
		this.Headers = Headers;
	}

	/**
	 * to get object
	 **/
	public Object getObject() {
		return object;
	}

	/**
	 * to set object
	 **/
	public void setObject(T object) {
		this.object = object;
	}

	/**
	 * to get Hashmap
	 **/
	public HashMap<String, Object> getHeaders() {
		return Headers;
	}

	/**
	 * to set Hashmap
	 **/
	public void setHeaders(HashMap<String, Object> herders) {
		Headers = herders;
	}

	/**
	 * to get the string of our WaveResponse
	 **/
	@Override
	public String toString() {
		return "WaveResponse [object=" + object + ", Headers=" + Headers.toString() + "]";
	}

}
