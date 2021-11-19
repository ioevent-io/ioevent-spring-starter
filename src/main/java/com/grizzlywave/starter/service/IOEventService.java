package com.grizzlywave.starter.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.domain.ParallelEventInfo;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IOEventService {
	
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	public void sendParallelEventInfo(WaveParallelEventInformation parallelEventInfo) {
		Message<WaveParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ParallelEventTopic")
				.setHeader(KafkaHeaders.MESSAGE_KEY, parallelEventInfo.getHeaders().get("Correlation_id")).build();

		kafkaTemplate.send(message);
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

	public List<String> getParalleListSource(IOEvent ioEvent) {
		List<String> result = new ArrayList<String>();
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
			if ((!targetEvent.name().equals("")) || (!targetEvent.suffix().equals(""))) {
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
		} else if (!ioEvent.endEvent().key().equals("")) {
			return IOEventType.END;
		} else {
			return IOEventType.TASK;
		}
	}

	public SourceEvent getSourceEventByName(IOEvent ioEvent,String sourceName) {
		for (SourceEvent sourceEvent :getSources(ioEvent)) {
			if (sourceName.equals(sourceEvent.name())) {
				return sourceEvent;
			}
		}
		return null;
	}

	public IOEventType checkTaskType(IOEvent ioEvent) {
		IOEventType type=IOEventType.TASK;
		
		if ((ioEvent.gatewayTarget().target().length != 0)||(ioEvent.gatewaySource().source().length!=0) ) {

			if (ioEvent.gatewayTarget().parallel()||ioEvent.gatewaySource().parallel()) {
			type=IOEventType.GATEWAY_PARALLEL;
			}
			else if (ioEvent.gatewayTarget().exclusive()||ioEvent.gatewaySource().exclusive()){
				type=IOEventType.GATEWAY_EXCLUSIVE;
				}
			}
		
		return type;
	}
}
