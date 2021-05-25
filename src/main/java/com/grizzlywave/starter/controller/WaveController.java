package com.grizzlywave.starter.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grizzlywave.starter.listener.Listener;
import com.grizzlywave.starter.model.IOEventBpmnPart;
import com.grizzlywave.starter.model.WaveBpmnPart;

@CrossOrigin(origins = "*")
@RestController
public class WaveController {

	@Autowired
	private List<WaveBpmnPart>	bpmnlist;
	
	@Autowired
	private List<Listener> listeners;
	
	@Autowired
	private List<IOEventBpmnPart>	iobpmnlist;
	@GetMapping("/ListBPMN")
	public List<WaveBpmnPart> getBpmnlist(){
		return bpmnlist;
	}
	@GetMapping("/IOeventBPMN")
	public List<IOEventBpmnPart> getlist(){
		return iobpmnlist;
	}
	
	@GetMapping("/listeners")
	public String getlistener() {
		String s="";
		for (Listener l:listeners) {
			s+=l.getTopic()+" : "+l.getBeanMethodPairs()+" ---- ";
		}
		
		return s; 
	}
}
