package com.grizzlywave.grizzlywavestarter.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grizzlywave.grizzlywavestarter.configuration.WaveBpmnPostProcessor;

@RestController
public class WaveController {

	
	
	@GetMapping("/BPMN")
	public Map<String,Object> getBpmnPart(){
		return WaveBpmnPostProcessor.bpmnPart;
		
	}
}
