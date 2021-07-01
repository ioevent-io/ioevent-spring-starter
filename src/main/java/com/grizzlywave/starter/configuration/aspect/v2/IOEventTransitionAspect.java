package com.grizzlywave.starter.configuration.aspect.v2;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.annotations.v2.IOEventResponse;
import com.grizzlywave.starter.annotations.v2.SendRecordInfo;
import com.grizzlywave.starter.annotations.v2.TargetEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.IOEventType;
import com.grizzlywave.starter.handler.WaveRecordInfo;
import com.grizzlywave.starter.logger.EventLogger;
import com.grizzlywave.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Configuration
public class IOEventTransitionAspect {

	private WaveRecordInfo waveRecordInfo= null;
	@Autowired
	private KafkaTemplate<String, Object> kafkaTemplate;

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WaveProperties waveProperties;

	@Autowired
	private IOEventService ioEventService;

	@Pointcut("execution(* com.grizzlywave.starter.service..*.*(..))")
	public void classpointcut() {
	}


	@Around(value = "classpointcut() && @annotation(audit)")
	public Object SendRecordInfoAspect(ProceedingJoinPoint pjp,SendRecordInfo audit) throws Throwable {
		this.waveRecordInfo =WaveRecordInfo.class.cast(pjp.getArgs()[0]);
		Object obj = pjp.proceed();
		return obj;
	}

	@AfterReturning(value = "@annotation(anno)", argNames = "jp, anno,return", returning = "return")
	public void transitionAspect(JoinPoint joinPoint, IOEvent ioEvent, Object object) throws Throwable {
		if (ioEvent.startEvent().key().equals("") && (ioEvent.endEvent().key().equals(""))) {
			EventLogger eventLogger = new EventLogger();
			eventLogger.startEventLog();
			StopWatch watch = new StopWatch();
			watch.start("IOEvent annotation Transition Aspect");
			String target = "";
			if (ioEvent.gatewayTarget().target().length != 0) {
				if (ioEvent.gatewayTarget().parallel()) {
					for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
						Message<Object> message = this.buildStartMessage(ioEvent, object, targetEvent,this.waveRecordInfo,eventLogger.getTimestamp(eventLogger.getStartTime()));
						kafkaTemplate.send(message);
						target += targetEvent.name() + ",";
					}
				} else if (ioEvent.gatewayTarget().exclusive()) {
					IOEventResponse<Object> ioEventResponse = IOEventResponse.class.cast(object);
					log.info("target of gateway" + ioEventResponse.getString());
					for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
						if (ioEventResponse.getString().equals(targetEvent.name())) {
							Message<Object> message = this.buildStartMessage(ioEvent, ioEventResponse.getBody(),
									targetEvent,this.waveRecordInfo,eventLogger.getTimestamp(eventLogger.getStartTime()));
							kafkaTemplate.send(message);
							target += targetEvent.name() + ",";
							log.info("sent to :"+targetEvent.name());
						}

					}

				}
			} else {
				for (TargetEvent targetEvent : ioEventService.getTargets(ioEvent)) {
					Message<Object> message = this.buildStartMessage(ioEvent, object, targetEvent,this.waveRecordInfo,eventLogger.getTimestamp(eventLogger.getStartTime()));
					kafkaTemplate.send(message);
					target += targetEvent.name() + ",";
				}
			}
			watch.stop();
			eventLogger.setting(this.waveRecordInfo.getId(), this.waveRecordInfo.getWorkFlowName(), ioEvent.name(), this.waveRecordInfo.getTargetName(), target, "Transition",
					object);
			eventLogger.stopEvent(watch.getTotalTimeMillis());
			String jsonObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(eventLogger);
			log.info(jsonObject);
		}
	}

	private Message<Object> buildStartMessage(IOEvent ioEvent, Object payload, TargetEvent targetEvent,WaveRecordInfo waveRecordInfo,Long startTime) {
		String topic = targetEvent.topic();
		if (topic.equals("")) {
			topic = ioEvent.topic();

		}
		return MessageBuilder.withPayload(payload).setHeader(KafkaHeaders.TOPIC, waveProperties.getPrefix() + topic)
				.setHeader(KafkaHeaders.MESSAGE_KEY, "999").setHeader(KafkaHeaders.PARTITION_ID, 0).setHeader("Process_Name",waveRecordInfo.getWorkFlowName())
				.setHeader("Correlation_id",waveRecordInfo.getId())
				.setHeader("EventType", IOEventType.TRANSITION.toString())
				.setHeader("source", ioEventService.getSourceNames(ioEvent))
				.setHeader("targetEvent", targetEvent.name()).setHeader("StepName", ioEvent.name()).setHeader("Start Time", startTime).build();
	}

}
