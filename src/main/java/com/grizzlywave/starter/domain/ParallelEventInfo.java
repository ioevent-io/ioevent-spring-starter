package com.grizzlywave.starter.domain;

import java.util.List;

public class ParallelEventInfo {
private String id;
private List<String> targets;



public ParallelEventInfo() {
	super();
	// TODO Auto-generated constructor stub
}



public ParallelEventInfo(String id, List<String> targets) {
	this.id = id;
	this.targets = targets;
}



public String getId() {
	return id;
}



public void setId(String id) {
	this.id = id;
}



public List<String> getTargets() {
	return targets;
}



public void setTargets(List<String> targets) {
	this.targets = targets;
}



@Override
public String toString() {
	return "CustomEvent [id=" + id + ", targets=" + targets + "]";
}


}
