package com.ioevent.starter.listener;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.ioevent.starter.configuration.context.AppContext;
import com.ioevent.starter.domain.IOEventHeaders;
import com.ioevent.starter.domain.IOEventMessageEventInformation;
import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.handler.RecordsHandler;
import com.ioevent.starter.service.IOEventContextHolder;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MessageListener {
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private List<Listener> listeners;
	@Autowired
	RecordsHandler recordsHandler;
	@Autowired
	private AppContext ctx;

	@Autowired
	private IOEventService ioEventService;

	@KafkaListener(topics = "ioevent-event-message-aggregation", containerFactory = "userKafkaListenerFactory", groupId = "#{'${spring.kafka.consumer.group-id:${ioevent.group_id:${spring.application.name:ioevent_default_groupid}}}'}")
	public void consumeMessageEvent(String s)
			throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		Gson gson = new Gson();
		IOEventMessageEventInformation iOEventMessageEventInformation = gson.fromJson(s,
				IOEventMessageEventInformation.class);
		if (iOEventMessageEventInformation != null && validMessage(iOEventMessageEventInformation)) {
			try {
				Object beanmObject = ctx.getApplicationContext()
						.getBean(Class.forName(iOEventMessageEventInformation.getClassName()));
				if (beanmObject != null) {
					new Thread(() -> {
						StopWatch watch = new StopWatch();
						watch.start((String) iOEventMessageEventInformation.getHeaders()
								.get(IOEventHeaders.CORRELATION_ID.toString()));
						IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo(
								iOEventMessageEventInformation.getHeaders()
										.get(IOEventHeaders.CORRELATION_ID.toString()).toString(),
								iOEventMessageEventInformation.getHeaders().get(IOEventHeaders.PROCESS_NAME.toString())
										.toString(),
								iOEventMessageEventInformation.getInputsArrived().toString(), watch,
								Long.valueOf(iOEventMessageEventInformation.getHeaders()
										.get(IOEventHeaders.START_INSTANCE_TIME.toString()).toString()),
								null);
						IOEventContextHolder.setContext(ioeventRecordInfo);

						try {
							invokeTargetMethod(iOEventMessageEventInformation.getMethod(), beanmObject,
									iOEventMessageEventInformation);
						} catch (Throwable e) {
							log.error(e.getMessage());
						}
					}).start();
				}
			} catch (Throwable e) {
				log.error("error while invoking method ");
			}

		} else {
			log.info("Message Event Not Completed, output arrived : " + //
					iOEventMessageEventInformation.getInputsArrived() + "message key arrived "
					+ iOEventMessageEventInformation.getMessageEventArrived());
		}

	}

	private boolean validMessage(IOEventMessageEventInformation iOEventMessageEventInformation) {
		return sameList(iOEventMessageEventInformation.getInputRequired(),
				iOEventMessageEventInformation.getInputsArrived())
				&& iOEventMessageEventInformation.getMessageEventArrived()
						.equals(iOEventMessageEventInformation.getMessageEventRequired());
	}

	/** method to invoke the method from a specific bean **/
	public void invokeTargetMethod(String methodName, Object beanmObject,
			IOEventMessageEventInformation messageEventInformation) throws Throwable {
		if (beanmObject != null) {

			for (Method met : beanmObject.getClass().getDeclaredMethods()) {
				if (met.getName().equals(methodName)) {
					Method method = met;
					try {
						method.invoke(ctx.getApplicationContext().getBean(beanmObject.getClass()), new Object[0]);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				}
			}
		}
	}

	public boolean sameList(List<String> firstList, List<String> secondList) {
		return (firstList.size() == secondList.size() && firstList.containsAll(secondList)
				&& secondList.containsAll(firstList));
	}

}
