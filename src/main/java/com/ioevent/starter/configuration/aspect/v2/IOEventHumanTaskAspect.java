package com.ioevent.starter.configuration.aspect.v2;

import com.ioevent.starter.annotations.*;
import com.ioevent.starter.configuration.properties.IOEventProperties;
import com.ioevent.starter.domain.IOEventBpmnPart;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventType;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.logger.EventLogger;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Date;
import java.util.Map;

@Aspect
@Configuration
@Slf4j
public class IOEventHumanTaskAspect {

    @Autowired
    private IOEventService ioEventService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private IOEventProperties iOEventProperties;

    @Value("${spring.application.name}")
    private String appName;

    @Around(value = "@annotation(anno) && execution(* *(@com.ioevent.starter.annotations.HumanInput (*), ..))",argNames = "joinPoint,anno")
    public Object aroundHumanTask(ProceedingJoinPoint joinPoint, IOEvent ioEvent) throws Throwable {
        Object humanInput = null;
        Object humanResponse = null;
        Object[] args = joinPoint.getArgs();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        for (int i= 0; i < parameters.length; i++) {
             if (parameters[i].isAnnotationPresent(HumanInput.class)) {
                humanInput = args[i];
             }
             if (parameters[i].isAnnotationPresent(HumanResponse.class)) {
                humanResponse = args[i];
             }
        }

        if(humanInput != null){
            //if the first argument is null so its the first call for the method and the human task will go to waiting for human response

            IOEventRecordInfo ioeventRecordInfo = IOEventContextHolder.getContext();
            IOFlow ioFlow = joinPoint.getTarget().getClass().getAnnotation(IOFlow.class);
            IOEventType ioEventType = ioEventService.checkTaskType(ioEvent);
            IOResponse<Object> response = ioEventService.getpayload(joinPoint, args[0]);
            Map<String, Object> headers = ioEventService.prepareHeaders(ioeventRecordInfo.getHeaderList(),
                    response.getHeaders());
            EventLogger eventLogger = new EventLogger();

            Message<Object> message = buildHumanTaskMessage(ioEvent, ioFlow, response, ioeventRecordInfo,
                    ioeventRecordInfo.getStartTime(), ioEventType, headers, "");

            Long eventTimeStamp = kafkaTemplate.send(message).get().getRecordMetadata().timestamp();
            eventLogger.setEndTime(eventLogger.getISODate(new Date(eventTimeStamp)));

            log.info(ioEvent.toString());

            //throw new InterruptedException("exception thrown intentionally to block the method");
            return "Method blocked";

            //return null;
        }
        else if(humanResponse != null){
            //if the second argument is null so its the second call for the method and we got the response from the user and the are
            //going to block the method it will continue
            log.info("entered to the second call");
            return joinPoint.proceed();
        }
        return null;
    }

    public Message<Object> buildHumanTaskMessage(IOEvent ioEvent,IOFlow ioFlow,IOResponse<Object> response,
                                                 IOEventRecordInfo ioeventRecordInfo, Long startTime, IOEventType ioEventType,
                                                 Map<String, Object> headers, String key) {
        log.info("entered to buildHumanTaskMessage");
        String apiKey = ioEventService.getApiKey(iOEventProperties, ioFlow);
        Map<String,String> outputs = new IOEventBpmnPart().addOutput(ioEvent,ioFlow,iOEventProperties.getPrefix());
        log.info(ioeventRecordInfo.getOutputConsumedName());
        return MessageBuilder.withPayload(response.getBody()).copyHeaders(headers)
                .setHeader(KafkaHeaders.TOPIC, iOEventProperties.getPrefix()+appName +"_"+"ioevent-human-task")
                .setHeader(KafkaHeaders.KEY, ioeventRecordInfo.getId())
                .setHeader(IOEventHeaders.MESSAGE_KEY.toString(), key)
                .setHeader(IOEventHeaders.PROCESS_NAME.toString(), ioeventRecordInfo.getWorkFlowName())
                .setHeader(IOEventHeaders.CORRELATION_ID.toString(), ioeventRecordInfo.getId())
                .setHeader(IOEventHeaders.EVENT_TYPE.toString(), ioEventType.toString())
                .setHeader(IOEventHeaders.INPUT.toString(), ioEventService.getInputNames(ioEvent))
                .setHeader("OUTPUTS", outputs)
                .setHeader("APPNAME",appName)
                .setHeader(IOEventHeaders.STEP_NAME.toString(), ioEvent.key())
                .setHeader(IOEventHeaders.API_KEY.toString(), apiKey)
                .setHeader(IOEventHeaders.START_TIME.toString(), startTime)
                .setHeader(IOEventHeaders.START_INSTANCE_TIME.toString(), ioeventRecordInfo.getInstanceStartTime())
                .setHeader(IOEventHeaders.IMPLICIT_START.toString(), false)
                .setHeader(IOEventHeaders.IMPLICIT_END.toString(), false).build();
    }
}
