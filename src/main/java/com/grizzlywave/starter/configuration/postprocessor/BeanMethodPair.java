package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.grizzlywave.starter.annotations.v2.IOEvent;

public class BeanMethodPair {

	private Object bean;
	private Method method;
	private IOEvent ioEvent;

	private List<String> sourceEventsArrived = new ArrayList<String>();
	public BeanMethodPair() {
	}

	public BeanMethodPair(Object bean, Method method,IOEvent ioEvent) {
		this.bean = bean;
		this.method = method;
		this.ioEvent=ioEvent; 
		
	}

	public Object getBean() {
		return bean;
	}

	public void setBean(Object bean) {
		this.bean = bean;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public IOEvent getIoEvent() {
		return ioEvent;
	}

	public void setIoEvent(IOEvent ioEvent) {
		this.ioEvent = ioEvent;
	}

	public List<String> getSourceEventsArrived() {
		return sourceEventsArrived;
	}
	public void addSourceEventsArrived(String sourceEvent) {
		this.sourceEventsArrived.add(sourceEvent);
	}
	public void setSourceEventsArrived(List<String> sourceEventsArrived) {
		this.sourceEventsArrived = sourceEventsArrived;
	}

	@Override
	public String toString() {
		return "BeanMethodPair [bean=" + bean + ", method=" + method + /*", ioEvent=" + ioEvent + */"]";
	}


}
