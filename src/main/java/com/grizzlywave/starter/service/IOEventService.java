package com.grizzlywave.starter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventHeaders;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.domain.WaveParallelEventInformation;

import lombok.extern.slf4j.Slf4j;

/**
 * class service for IOEvent,
 */
@Slf4j
@Service
public class IOEventService {

	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	/**
	 * This is a kafka producer which send parallel events info to
	 * ParallelEventTopic topic
	 * 
	 * @param parallelEventInfo for the parallel event information,
	 */
	public void sendParallelEventInfo(WaveParallelEventInformation parallelEventInfo) {
		Message<WaveParallelEventInformation> message = MessageBuilder.withPayload(parallelEventInfo)
				.setHeader(KafkaHeaders.TOPIC, "ParallelEventTopic").setHeader(KafkaHeaders.MESSAGE_KEY,
						parallelEventInfo.getHeaders().get(IOEventHeaders.CORRELATION_ID.toString()))
				.build();

		kafkaTemplate.send(message);
	}

	/**
	 * method returns all sources names of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of sources names,
	 */
	public List<String> getSourceNames(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();

		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
				if (!StringUtils.isBlank(sourceEvent.value())) {
					result.add(sourceEvent.value());
				} else {
					result.add(sourceEvent.key());
				}
			}
		}

		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
				if (!StringUtils.isBlank(sourceEvent.value())) {
					result.add(sourceEvent.value());
				} else {
					result.add(sourceEvent.key());
				}
			}
		}
		return result;
	}

	/**
	 * method returns all parallel source names of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of sources names,
	 */
	public List<String> getParalleListSource(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();
		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
				if (!StringUtils.isBlank(sourceEvent.value())) {
					result.add(sourceEvent.value());
				}else {
					result.add(sourceEvent.key());
				}
			}
		}
		return result;
	}

	/**
	 * method returns all targets names of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of targets names,
	 */
	public List<String> getTargetNames(IOEvent ioEvent) {
		List<String> result = new ArrayList<>();

		for (TargetEvent targetEvent : ioEvent.target()) {
			if (!StringUtils.isBlank(targetEvent.key() + targetEvent.value())) {
				if (!StringUtils.isBlank(targetEvent.value())) {
					result.add(targetEvent.value());
				} else {
					result.add(targetEvent.key());
				}
			}
		}

		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!StringUtils.isBlank(targetEvent.key() + targetEvent.value())) {
				if (!StringUtils.isBlank(targetEvent.value())) {
					result.add(targetEvent.value());
				} else {
					result.add(targetEvent.key());
				}
			}
		}
		return result;
	}

	/**
	 * method returns all target Event of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of TargetEvent Object ,
	 */
	public List<TargetEvent> getTargets(IOEvent ioEvent) {
		List<TargetEvent> result = new ArrayList<>();

		for (TargetEvent targetEvent : ioEvent.target()) {
			if (!StringUtils.isBlank(targetEvent.key() + targetEvent.value() + targetEvent.suffix())) {
				result.add(targetEvent);
			}
		}

		for (TargetEvent targetEvent : ioEvent.gatewayTarget().target()) {
			if (!StringUtils.isBlank(targetEvent.key() + targetEvent.value() + targetEvent.suffix())) {
				result.add(targetEvent);
			}
		}
		return result;
	}

	/**
	 * method returns all targets of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of TargetEvent Object ,
	 */
	public List<SourceEvent> getSources(IOEvent ioEvent) {
		List<SourceEvent> result = new ArrayList<>();

		for (SourceEvent sourceEvent : ioEvent.source()) {
			if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
				result.add(sourceEvent);
			}
		}

		for (SourceEvent sourceEvent : ioEvent.gatewaySource().source()) {
			if (!StringUtils.isBlank(sourceEvent.key() + sourceEvent.value())) {
				result.add(sourceEvent);
			}
		}
		return result;
	}

	/**
	 * method returns all topics of @IOEvent annotation,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of Topics names ,
	 */

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

	/**
	 * method returns all source topics of @IOEvent definition,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return list of Topics names ,
	 */

	public List<String> getSourceTopic(IOEvent ioEvent, IOFlow ioFlow) {
		List<String> result = new ArrayList<>();
		if ((ioFlow != null) && !StringUtils.isBlank(ioFlow.topic())) {
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

	/**
	 * method returns if two lists are equal
	 * 
	 * @param firstList  list of String,
	 * @param secondList list of String,
	 * @return boolean ,
	 */
	public boolean sameList(List<String> firstList, List<String> secondList) {
		return (firstList.size() == secondList.size() && firstList.containsAll(secondList)
				&& secondList.containsAll(firstList));
	}

	/**
	 * method returns event type from the IOEvent annotation
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return IOEventType ,
	 */
	public IOEventType getIOEventType(IOEvent ioEvent) {
		if (!StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())) {
			return IOEventType.START;
		} else if (!StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())) {
			return IOEventType.END;
		} else {
			return IOEventType.TASK;
		}
	}

	/**
	 * method returns if the IOEvent annotation is of a Start Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isStart(IOEvent ioEvent) {
		return (!StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())
				&& (!getTargets(ioEvent).isEmpty()));

	}

	/**
	 * method returns if the IOEvent annotation is of a End Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isEnd(IOEvent ioEvent) {
		return (!StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())
				&& (!getSources(ioEvent).isEmpty()));
	}

	/**
	 * method returns if the IOEvent annotation is of a Implicit Task Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isImplicitTask(IOEvent ioEvent) {
		return ((getSources(ioEvent).isEmpty() || getTargets(ioEvent).isEmpty())
				&& (StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())
						&& StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())));

	}

	/**
	 * method returns if the IOEvent annotation is of a Task Event
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return boolean ,
	 */
	public boolean isTransition(IOEvent ioEvent) {
		return (StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())
				&& StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())
				&& !getSources(ioEvent).isEmpty() && !getTargets(ioEvent).isEmpty());
	}

	/**
	 * method returns source event of @IOEvent by name,
	 * 
	 * @param ioEvent    for the IOEvent annotation,
	 * @param sourceName for the source event name
	 * @return SourceEvent ,
	 */
	public SourceEvent getSourceEventByName(IOEvent ioEvent, String sourceName) {
		for (SourceEvent sourceEvent : getSources(ioEvent)) {
			if (sourceName.equals(sourceEvent.key())) {
				return sourceEvent;
			}
		}
		return null;
	}

	/**
	 * method returns Task specific type from the IOEvent annotation
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return IOEventType ,
	 */
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

	/**
	 * method returns ID generated from @IOEvent elements ,
	 * 
	 * @param ioEvent for the IOEvent annotation,
	 * @return String of ID generated ,
	 */
	public String generateID(IOEvent ioEvent) {

		return ioEvent.key().replaceAll("[^a-zA-Z ]", "").toLowerCase().replace(" ", "") + "-"
				+ getSourceNames(ioEvent).hashCode() + "-" + getTargetNames(ioEvent).hashCode();
	}

	/**
	 * method returns ProcessName from @IOEvent ,@IOFlow and recordProcessName ,
	 * 
	 * @param ioEvent           for the IOEvent annotation,
	 * @param ioFlow            for the IOFlow annotation,
	 * @param recordProcessName for the process name consumed from record ,
	 * @return String of ProcessName ,
	 */
	public String getProcessName(IOEvent ioEvent, IOFlow ioFlow, String recordProcessName) {
		if (!StringUtils.isBlank(recordProcessName)) {
			return recordProcessName;

		} else if (!StringUtils.isBlank(ioEvent.startEvent().key() + ioEvent.startEvent().value())) {
			if (!StringUtils.isBlank(ioEvent.startEvent().value())) {
				return ioEvent.startEvent().value();
			}
			return ioEvent.startEvent().key();
		} else if (!StringUtils.isBlank(ioEvent.endEvent().key() + ioEvent.endEvent().value())) {
			if (!StringUtils.isBlank(ioEvent.endEvent().value())) {
				return ioEvent.endEvent().value();
			}
			return ioEvent.endEvent().key();

		} else if (!Objects.isNull(ioFlow)) {
			return ioFlow.name();
		}
		return "";
	}

	/**
	 * method returns target topic from @IOEvent ,@IOFlow and targetEventTopic ,
	 * 
	 * @param ioEvent          for the IOEvent annotation,
	 * @param ioFlow           for the IOFlow annotation,
	 * @param targetEventTopic for the target Event Topic name,
	 * @return String of TopicName ,
	 */
	public String getTargetTopicName(IOEvent ioEvent, IOFlow ioFlow, String targetEventTopic) {
		if (!StringUtils.isBlank(targetEventTopic)) {
			return targetEventTopic;
		} else if (!StringUtils.isBlank(ioEvent.topic())) {
			return ioEvent.topic();
		} else if ((ioFlow != null) && !StringUtils.isBlank(ioFlow.topic())) {
			return ioFlow.topic();

		} else {
			return "";
		}
	}

	/**
	 * method returns ApiKey from @IOFlow and WaveProperties ,
	 * 
	 * @param ioFlow         for the IOFlow annotation,
	 * @param WaveProperties for the IOEvent custom properties value ,
	 * @return String of ApiKey ,
	 */
	public String getApiKey(WaveProperties waveProperties, IOFlow ioFlow) {
		if ((!Objects.isNull(ioFlow)) && (StringUtils.isNotBlank(ioFlow.apiKey()))) {
			return ioFlow.apiKey();
		} else if (StringUtils.isNotBlank(waveProperties.getApikey())) {
			return waveProperties.getApikey();
		}
		return "";
	}

	public String getTargetKey(TargetEvent targetEvent) {
			if (!StringUtils.isBlank(targetEvent.value())) {
				return targetEvent.value();
			} else {
			return	targetEvent.key();
			}
		}
	}

