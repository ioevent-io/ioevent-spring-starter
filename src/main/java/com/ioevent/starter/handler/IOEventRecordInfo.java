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




package com.ioevent.starter.handler;






import java.util.List;

import org.apache.kafka.common.header.Header;
import org.springframework.util.StopWatch;

/**
 * class for the record event information consumed from event
 */
public class IOEventRecordInfo {
	private String id;

	private String workFlowName;

	private String outputConsumedName;
	private List<Header> headerList;

	private StopWatch watch;
	private Long instanceStartTime;
	private Long startTime = System.currentTimeMillis();
	private String lastEventEndTime ;

	public IOEventRecordInfo() {
	}

	public IOEventRecordInfo(String id, String workFlowName, String outputConsumedName, StopWatch watch,
			Long instanceStartTime,String lastEventEndTime) {
		this.id = id;
		this.workFlowName = workFlowName;
		this.outputConsumedName = outputConsumedName;
		this.watch = watch;
		this.instanceStartTime = instanceStartTime;
		this.lastEventEndTime = lastEventEndTime;
	}

	public IOEventRecordInfo(String id, String workFlowName, String outputConsumedName, List<Header> headerList,
			Long instanceStartTime,String lastEventEndTime) {
		super();
		this.id = id;
		this.workFlowName = workFlowName;
		this.outputConsumedName = outputConsumedName;
		this.headerList = headerList;
		this.instanceStartTime = instanceStartTime;
		this.lastEventEndTime = lastEventEndTime;

	}

	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getWorkFlowName() {
		return workFlowName;
	}

	public void setWorkFlowName(String workFlowName) {
		this.workFlowName = workFlowName;
	}

	public String getOutputConsumedName() {
		return outputConsumedName;
	}

	public void setOutputConsumedName(String outputConsumedName) {
		this.outputConsumedName = outputConsumedName;
	}

	public List<Header> getHeaderList() {
		return headerList;
	}

	public void setHeaderList(List<Header> headerList) {
		this.headerList = headerList;
	}

	public StopWatch getWatch() {
		return watch;
	}

	public void setWatch(StopWatch watch) {
		this.watch = watch;
	}

	public Long getInstanceStartTime() {
		return instanceStartTime;
	}

	public void setInstanceStartTime(Long instanceStartTime) {
		this.instanceStartTime = instanceStartTime;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}
	public String getLastEventEndTime() {
		return lastEventEndTime;
	}

	public void setLastEventEndTime(String lastEventEndTime) {
		this.lastEventEndTime = lastEventEndTime;
	}
}
