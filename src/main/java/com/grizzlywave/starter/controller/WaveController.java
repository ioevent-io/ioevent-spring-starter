package com.grizzlywave.starter.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
public class WaveController {

	@Autowired
	 List<Map<String, Object>>	bpmnlist;
	
	@GetMapping("/ListBPMN")
	public List<Map<String, Object>> getBpmnlist(){
	//	return WaveBpmnPostProcessor.bpmnlist;
		return bpmnlist;
	}
}
