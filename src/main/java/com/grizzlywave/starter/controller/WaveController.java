package com.grizzlywave.starter.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grizzlywave.starter.domain.IOEventBpmnPart;
import com.grizzlywave.starter.service.TopicServices;

/**
 * class for the controller of the IOEvent starter,
 */
@CrossOrigin(origins = "*")
@RestController
public class WaveController {

	@Autowired
	private TopicServices topicServices;

	@Autowired
	private List<IOEventBpmnPart> iobpmnlist;

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
	@GetMapping("/WaveTopics")
	public List<String> getWaveTopics() throws InterruptedException, ExecutionException {
		return topicServices.getAllTopic();
	}
}
