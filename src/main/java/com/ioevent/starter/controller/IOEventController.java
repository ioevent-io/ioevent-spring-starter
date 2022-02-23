package com.ioevent.starter.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ioevent.starter.domain.IOEventBpmnPart;
import com.ioevent.starter.service.TopicServices;

/**
 * class for the controller of the IOEvent starter,
 */
@CrossOrigin(origins = "*")
@RestController
public class IOEventController {

	@Autowired
	private TopicServices topicServices;

	@Autowired
	private List<IOEventBpmnPart> iobpmnlist;
	
	@Autowired
	private Set<String> apiKeys;
	/**
	 * Method that return all BPMN parts of processes,
	 * 
	 * @return list of IOEventBpmnPart Object,
	 */
	@GetMapping("/IOeventBPMN")
	public List<IOEventBpmnPart> getlist() {
		return iobpmnlist;
	}

	/**
	 * Method that return all Topics,
	 * 
	 * @return list of topics names,
	 */
	@GetMapping("/IOEventTopics")
	public List<String> getIOEventTopics() throws InterruptedException, ExecutionException {
		return topicServices.getAllTopic();
	}
	/**
	 * Method that return all API-Keys used in application,
	 * 
	 * @return list of API-Key names,
	 */
	@GetMapping("/IOEventApiKeys")
	public List<String> getIOEventApiKeys() {
		return new ArrayList<>(apiKeys);
	}
}
