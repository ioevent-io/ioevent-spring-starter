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

	private String targetName;
	private List<Header> headerList;

	private StopWatch watch;
	private Long instanceStartTime;
	private Long startTime = System.currentTimeMillis();

	public IOEventRecordInfo() {
	}

	public IOEventRecordInfo(String id, String workFlowName, String targetName, StopWatch watch,
			Long instanceStartTime) {
		this.id = id;
		this.workFlowName = workFlowName;
		this.targetName = targetName;
		this.watch = watch;
		this.instanceStartTime = instanceStartTime;
	}

	public IOEventRecordInfo(String id, String workFlowName, String targetName, List<Header> headerList,
			Long instanceStartTime) {
		super();
		this.id = id;
		this.workFlowName = workFlowName;
		this.targetName = targetName;
		this.headerList = headerList;
		this.instanceStartTime = instanceStartTime;

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

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
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

}
