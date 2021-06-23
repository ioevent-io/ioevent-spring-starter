package com.grizzlywave.starter.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SendRecordInfo;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IOEventService {
	
	@SendRecordInfo
	public  String sendWaveRecordInfo(WaveRecordInfo waveRecordInfo){
		return "RecordInfo sent";
	}

	public List<String> getSourceNames(IOEvent ioEvent) {
		List<String> result = new ArrayList<String>();

		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!sourceEvent.name().equals("")) {
				result.add(sourceEvent.name());
			}
		}

		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!sourceEvent.name().equals("")) {
				result.add(sourceEvent.name());
			}
		}
		return result;
	}

	public List<String> getTargetNames(IOEvent ioEvent) {
		List<String> result = new ArrayList<String>();

		for (TargetEvent targetEvent : ioEvent.target()) {
			if (!targetEvent.name().equals("")) {
				result.add(targetEvent.name());
			}
		}

		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!targetEvent.name().equals("")) {
				result.add(targetEvent.name());
			}
		}
		return result;
	}

	public List<TargetEvent> getTargets(IOEvent ioEvent) {
		List<TargetEvent> result = new ArrayList<TargetEvent>();

		for (TargetEvent targetEvent : ioEvent.target()) {
			if (!targetEvent.name().equals("")) {
				result.add(targetEvent);
			}
		}

		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!targetEvent.name().equals("")) {
				result.add(targetEvent);
			}
		}
		return result;
	}

	public List<SourceEvent> getSources(IOEvent ioEvent) {
		List<SourceEvent> result = new ArrayList<SourceEvent>();

		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!sourceEvent.name().equals("")) {
				result.add(sourceEvent);
			}
		}

		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!sourceEvent.name().equals("")) {
				result.add(sourceEvent);
			}
		}
		return result;
	}

	public List<String> getTopics(IOEvent ioEvent) {
		List<String> result = new ArrayList<String>();
		if (!ioEvent.topic().equals("")) {
			result.add(ioEvent.topic());
		}
		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!sourceEvent.topic().equals("")) {
				result.add(sourceEvent.topic());
			}
		}
		for (TargetEvent targetEvent : ioEvent.target()) {
			if (!targetEvent.topic().equals("")) {
				result.add(targetEvent.topic());
			}
		}
		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!sourceEvent.topic().equals("")) {
				result.add(sourceEvent.topic());
			}
		}
		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!targetEvent.topic().equals("")) {
				result.add(targetEvent.topic());
			}
		}
		return result;
	}

	public List<String> getSourceTopic(IOEvent ioEvent) {
		List<String> result = new ArrayList<String>();
		if (!ioEvent.topic().equals("")) {
			result.add(ioEvent.topic());
		}
		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!sourceEvent.topic().equals("")) {
				result.add(sourceEvent.topic());
			}
		}

		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!sourceEvent.topic().equals("")) {
				result.add(sourceEvent.topic());
			}
		}

		return result;
	}

	public boolean sameList(List<String> firstList, List<String> secondList) {
		return (firstList.size() == secondList.size() && firstList.containsAll(secondList)
				&& secondList.containsAll(firstList));
	}

	public IOEventType getIOEventType(IOEvent ioEvent) {
		if (!ioEvent.startEvent().key().equals("")) {
			return IOEventType.START;
		}
		else if (!ioEvent.endEvent().key().equals("")) {
			return IOEventType.END;
		}
		else {
			return IOEventType.TRANSITION;
		}
	}
}
