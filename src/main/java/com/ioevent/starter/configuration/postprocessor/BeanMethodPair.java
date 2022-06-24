/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




package com.ioevent.starter.configuration.postprocessor;






import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.ioevent.starter.annotations.IOEvent;

public class BeanMethodPair {

	private Object bean;
	private Method method;
	private IOEvent ioEvent;

	private List<String> inputEventsArrived = new ArrayList<String>();
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

	public List<String> getInputEventsArrived() {
		return inputEventsArrived;
	}
	public void addInputEventsArrived(String inputEvent) {
		this.inputEventsArrived.add(inputEvent);
	}
	public void setInputEventsArrived(List<String> inputEventsArrived) {
		this.inputEventsArrived = inputEventsArrived;
	}

	@Override
	public String toString() {
		return "BeanMethodPair [bean=" + bean + ", method=" + method + /*", ioEvent=" + ioEvent + */"]";
	}


}
