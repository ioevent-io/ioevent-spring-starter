package com.ioevent.starter.listener;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.ioevent.starter.configuration.context.AppContext;
import com.ioevent.starter.domain.IOTimerEvent;
import com.ioevent.starter.handler.RecordsHandler;
import com.ioevent.starter.service.IOEventService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IOEventTimerListener {
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private List<Listener> listeners;
	@Autowired
	RecordsHandler recordsHandler;
	@Autowired
	private AppContext ctx;

	@Autowired
	private IOEventService ioEventService;
	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private Executor asyncExecutor;

	@KafkaListener(topics = "ioevent-timer-execute", containerFactory = "userKafkaListenerFactory", groupId = "#{'${spring.kafka.consumer.group-id:${ioevent.group_id:${spring.application.name:ioevent_default_groupid}}}'}")
	public void consumeParallelEvent(String s)
			throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, SecurityException {
		Gson gson = new Gson();
		IOTimerEvent ioeventTimerEvent = gson.fromJson(s, IOTimerEvent.class);
		if (ioeventTimerEvent != null && ioeventTimerEvent.getAppName().equals(appName)) {

			try {
				Object beanmObject = ctx.getApplicationContext().getBean(Class.forName(ioeventTimerEvent.getBean()));
				if (beanmObject != null) {
					asyncExecutor.execute(() -> {
						try {
							this.invokeTargetMethod(ioeventTimerEvent.getMethodName(), beanmObject, ioeventTimerEvent);
						} catch (Throwable e) {
							log.error(e.getMessage());
						}
					});
				}
			} catch (Exception e) {
				log.error(e.getMessage());
			}
		}
	}

	/** method to invoke the method from a specific bean **/
	public void invokeTargetMethod(String methodName, Object beanmObject, IOTimerEvent ioeventTimerEvent)
			throws Throwable {
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


}
