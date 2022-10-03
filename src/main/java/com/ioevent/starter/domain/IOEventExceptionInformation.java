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
package com.ioevent.starter.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ioevent.starter.annotations.IOEvent;

public class IOEventExceptionInformation {
	private Boolean errorBoundryEvent;
	private Map<String, String> outputEvent = new HashMap<>();
	private String errorType;

	public IOEventExceptionInformation() {
		super();
		this.errorBoundryEvent = false;
		this.errorType="";
	}

	public IOEventExceptionInformation(Boolean errorBoundryEvent, Map<String, String> outputEvent) {
		super();
		this.errorBoundryEvent = errorBoundryEvent;
		this.outputEvent = outputEvent;
	}

	public IOEventExceptionInformation(IOEvent ioEvent) {
		
		this.errorBoundryEvent = !ioEvent.exception().output().key().isEmpty() ;
		
		if(!StringUtils.isBlank(Arrays.toString(ioEvent.exception().exception()))) {
			this.errorType = Arrays.toString(ioEvent.exception().exception());
		}else {
			this.errorType = "";
		}
		if (StringUtils.isBlank(ioEvent.exception().endEvent().value()+ioEvent.exception().endEvent().key()) &&
				!StringUtils.isBlank(ioEvent.exception().output().value()+ioEvent.exception().output().key())
				) {
				
			if(StringUtils.isBlank(ioEvent.exception().output().value())) {
				this.outputEvent.put(ioEvent.exception().output().key(), ioEvent.exception().output().topic());
			}else {
				this.outputEvent.put(ioEvent.exception().output().value(), ioEvent.exception().output().topic());
			}
		}
	}

	public Boolean getErrorBoundryEvent() {
		return errorBoundryEvent;
	}

	public void setErrorBoundryEvent(Boolean errorBoundryEvent) {
		this.errorBoundryEvent = errorBoundryEvent;
	}

	public Map<String, String> getOutputEvent() {
		return outputEvent;
	}

	public void setOutputEvent(Map<String, String> outputEvent) {
		this.outputEvent = outputEvent;
	}

	@Override
	public String toString() {
		return "IOEventExceptionInformation [errorBoundryEvent=" + errorBoundryEvent + ", outputEvent=" + outputEvent
				+ "]";
	}

	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}
}
