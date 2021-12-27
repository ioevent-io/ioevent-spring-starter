package com.grizzlywave.starter.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.IOFlow;
import com.grizzlywave.starter.annotations.v2.SourceEvent;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.domain.IOEventType;
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
		List<String> result = new ArrayList<>();

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
		List<String> result = new ArrayList<>();
		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!sourceEvent.name().equals("")) {
				result.add(sourceEvent.name());
			}
		}
		return result;
	}

	public List<String> getTargetNames(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();

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
		List<TargetEvent> result = new ArrayList<>();

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
		List<SourceEvent> result = new ArrayList<>();

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
		List<String> result = new ArrayList<>();
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

	public List<String> getSourceTopic(IOEvent ioEvent, IOFlow ioFlow) {
		List<String> result = new ArrayList<>();
		if ((ioFlow!=null)&&!StringUtils.isBlank(ioFlow.topic())) {
			result.add(ioFlow.topic());
		}
		if (!StringUtils.isBlank(ioEvent.topic())) {
			result.add(ioEvent.topic());
		}
		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!StringUtils.isBlank(sourceEvent.topic())) {
				result.add(sourceEvent.topic());
			}
		}

		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!StringUtils.isBlank(sourceEvent.topic())) {
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
		if (!StringUtils.isBlank(ioEvent.startEvent().key())) {
			return IOEventType.START;
		} else if (!StringUtils.isBlank(ioEvent.endEvent().key())) {
			return IOEventType.END;
		} else {
			return IOEventType.TASK;
		}
	}

	public boolean isStart(IOEvent ioEvent) {
		return (!StringUtils.isBlank(ioEvent.startEvent().key()) && (!getTargets(ioEvent).isEmpty()));

	}

	public boolean isEnd(IOEvent ioEvent) {
		return (!StringUtils.isBlank(ioEvent.endEvent().key()) && (!getSources(ioEvent).isEmpty()));
	}

	public boolean isImplicitTask(IOEvent ioEvent) {
		return ((getSources(ioEvent).isEmpty() || getTargets(ioEvent).isEmpty())
				&& (StringUtils.isBlank(ioEvent.startEvent().key()) && StringUtils.isBlank(ioEvent.endEvent().key())));

	}

	public boolean isTransition(IOEvent ioEvent) {
		return (StringUtils.isBlank(ioEvent.startEvent().key()) && StringUtils.isBlank(ioEvent.endEvent().key())
				&& !getSources(ioEvent).isEmpty() && !getTargets(ioEvent).isEmpty());
	}

	public SourceEvent getSourceEventByName(IOEvent ioEvent, String sourceName) {
		for (SourceEvent sourceEvent : getSources(ioEvent)) {
			if (sourceName.equals(sourceEvent.name())) {
				return sourceEvent;
			}
		}
		return null;
	}

	public IOEventType checkTaskType(IOEvent ioEvent) {
		IOEventType type = IOEventType.TASK;

		if ((ioEvent.gatewayTarget().target().length != 0) || (ioEvent.gatewaySource().source().length != 0)) {

			if (ioEvent.gatewayTarget().parallel() || ioEvent.gatewaySource().parallel()) {
				type = IOEventType.GATEWAY_PARALLEL;
			} else if (ioEvent.gatewayTarget().exclusive() || ioEvent.gatewaySource().exclusive()) {
				type = IOEventType.GATEWAY_EXCLUSIVE;
			}
		}

		return type;
	}

	public String generateID(IOEvent ioEvent) {

		return ioEvent.name().replaceAll("[^a-zA-Z ]", "").toLowerCase().replace(" ", "") + "-"
				+ getSourceNames(ioEvent).hashCode() + "-" + getTargetNames(ioEvent).hashCode();
	}

	public String getProcessName(IOEvent ioEvent, IOFlow ioFlow, String recordProcessName) {
		if (!StringUtils.isBlank(recordProcessName)) {
			return recordProcessName;

		} else if (!StringUtils.isBlank(ioEvent.startEvent().key())) {
			return ioEvent.startEvent().key();
		} else if (!StringUtils.isBlank(ioEvent.endEvent().key())) {
			return ioEvent.endEvent().key();

		}
		return ioFlow.name();
	}

	public String getTargetTopicName(IOEvent ioEvent, IOFlow ioFlow, String targetEventTopic) {
		if (!StringUtils.isBlank(targetEventTopic)) {
			return targetEventTopic;
		} else if (!StringUtils.isBlank(ioEvent.topic())) {
			return ioEvent.topic();
		} else if ((ioFlow!=null)&&!StringUtils.isBlank(ioFlow.topic())) {
			return ioFlow.topic();

		} else {
			return "";
		}
	}
}
