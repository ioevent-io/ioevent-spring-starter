package com.grizzlywave.starter.handler;

import java.util.List;

import org.apache.kafka.common.header.Header;
import org.springframework.util.StopWatch;

public class WaveRecordInfo {
	private String id;

	private String workFlowName;
	
	private String targetName;
	private List<Header> headerList;
	
	private StopWatch watch;

	private Long startTime=System.currentTimeMillis();
	
	

	public WaveRecordInfo() {
	}

	public WaveRecordInfo(String id, String workFlowName, String targetName,StopWatch watch) {
		this.id = id;
		this.workFlowName = workFlowName;
		this.targetName = targetName;
		this.watch = watch;
	}

	public WaveRecordInfo(String id, String workFlowName, String targetName, List<Header> headerList) {
		super();
		this.id = id;
		this.workFlowName = workFlowName;
		this.targetName = targetName;
		this.headerList = headerList;
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
	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

}
