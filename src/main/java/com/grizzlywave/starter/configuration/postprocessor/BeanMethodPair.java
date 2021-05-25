package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;

import com.grizzlywave.starter.annotations.v2.IOEvent;

public class BeanMethodPair {

	private Object bean;
	private Method method;
	private IOEvent ioEvent;
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

	@Override
	public String toString() {
		return "BeanMethodPair [bean=" + bean + ", method=" + method + /*", ioEvent=" + ioEvent + */"]";
	}


}
