package com.grizzlywave.starter.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grizzlywave.starter.model.WaveBpmnPart;

@CrossOrigin(origins = "*")
@RestController
public class WaveController {

	@Autowired
	private List<WaveBpmnPart>	bpmnlist;
	
	@GetMapping("/ListBPMN")
	public List<WaveBpmnPart> getBpmnlist(){
	//	return WaveBpmnPostProcessor.bpmnlist;
		return bpmnlist;
	}
}
