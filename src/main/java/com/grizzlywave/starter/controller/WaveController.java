package com.grizzlywave.starter.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grizzlywave.starter.domain.IOEventBpmnPart;
import com.grizzlywave.starter.service.TopicServices;

@CrossOrigin(origins = "*")
@RestController
public class WaveController {

	@Autowired
	private TopicServices topicServices;
	
	@Autowired
	private List<IOEventBpmnPart> iobpmnlist;

	@GetMapping("/IOeventBPMN")
	public List<IOEventBpmnPart> getlist() {
		return iobpmnlist;
	}

	@GetMapping("/WaveTopics")
	public List<String> getWaveTopics() throws InterruptedException, ExecutionException {
		return topicServices.getAllTopic();
	}
}
