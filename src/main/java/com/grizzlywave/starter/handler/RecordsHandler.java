package com.grizzlywave.starter.handler;

import java.lang.reflect.Method;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;

import com.grizzlywave.starter.configuration.context.AppContext;

import lombok.extern.slf4j.Slf4j;

/** Records handler to invoke method when consuming records from topic */
@Slf4j
public class RecordsHandler {
	
	
	@Autowired
	AppContext ctx ;
	
	public RecordsHandler() {
	}

	public void process(ConsumerRecords<String, String> consumerRecords, Object bean, Method method) throws Throwable {
		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {

			this.doInvoke(method, bean, consumerRecord.value());
		}

	}

	public void doInvoke(Method method, Object bean, Object args) throws Throwable {
		Object beanmObject = ctx.getApplicationContext().getBean(bean.getClass());
		if (beanmObject!=null) {
			
		
		for (Method met : beanmObject.getClass().getDeclaredMethods()) {
			if (met.getName().equals(method.getName())) {
				 met.invoke(ctx.getApplicationContext().getBean(bean.getClass()), args);
			} 
		}
		
	}}
}
