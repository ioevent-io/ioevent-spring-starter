package com.ioevent.starter.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IOEventMessageEventInformation {

	private String value;
	private List<String> inputsArrived = new ArrayList<>();
	private Map<String, Object> payloadMap = new HashMap<>();
	private String listenerTopic;
	private String method;
	private String className;
	private List<String> inputRequired;
	private Map<String, Object> headers = new HashMap<>();
}
