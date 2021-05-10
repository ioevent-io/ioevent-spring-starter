package com.grizzlywave.starter.handler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.listener.adapter.HandlerAdapter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

/** Listener handler under dev */
public class FileWritingRecordsHandler implements ConsumerRecordsHandler<String, String> {

	@Autowired
	private RecordMessageConverter conv;
	@Autowired
	MethodInterceptor methodInterceptor;
	@Autowired
	private ApplicationContext applicationContext;

	public FileWritingRecordsHandler() {
	}

	protected Object doInvoke(Method method, Object bean, Object args, Message<String> message,
			Consumer<String, String> consumer) throws Throwable {
		ReflectionUtils.makeAccessible(method);
		InvocableHandlerMethod handler = new InvocableHandlerMethod(bean, method);
		HandlerAdapter handlerMethod = new HandlerAdapter(handler);
		AspectJProxyFactory factory = new AspectJProxyFactory(bean);
		factory.getInterceptorsAndDynamicInterceptionAdvice(method, bean.getClass());
		// return handlerMethod.invoke(message, args, null, consumer);
		// return handlerMethod.invoke(message, args);
		// Object myService = applicationContext.getBean(bean.getClass());
		// for (Method method4 : bean.getClass().getDeclaredMethod(method.getName(),
		// args.getClass())) {
		// Create the Proxy Factory
	/*	AspectJProxyFactory proxyFactory = new AspectJProxyFactory(bean);
		Class<?> clazz = Class.forName(bean.getClass().getName());
		Object instance = clazz.newInstance();
		return method.invoke(instance, args);
*/
		/*
		 * for(Method met : instance.getClass().getMethods()) { if
		 * (met.getName().equals(method.getName())) return method.invoke(instance,
		 * args); }
		 */
		// Add Aspect class to the factory
		// proxyFactory.addAspect(WaveTransitionAspect.class);

		// Get the proxy object
//	return      method.invoke(proxyFactory.getProxy(),args);

		// Method method4 = bean.getClass().getDeclaredMethod(method.getName(),
		// args.getClass());
//				return method4.invoke(bean, args);

		// return
		// Proxy.getInvocationHandler(factory.getTargetSource()).invoke(factory.getTargetSource(),
		// method, (Object[]) args);
		 return method.invoke(bean, args);
		// return ReflectionUtils.invokeMethod(method, bean, args);

	}

	@Override
	public void process(ConsumerRecords<String, String> consumerRecords, Object bean, Method method,
			Consumer<String, String> consumer) throws Throwable {
		for (ConsumerRecord<String, String> consumerRecord : consumerRecords) {
			Map<String, Object> headersMap = new HashMap<String, Object>();
			consumerRecord.headers().forEach(h -> headersMap.put(h.key(), h.value()));
			consumerRecord.headers().forEach(h -> System.out.println(new String(h.value())));
			System.out.println("--" + consumerRecord.toString());
			System.out.println(consumerRecord.offset() + "--" + consumerRecord.partition());
			Message<String> message = MessageBuilder.withPayload(consumerRecord.value()).copyHeaders(headersMap)
					.build();
			// method.invoke(bean, consumerRecord.value());
			this.doInvoke(method, bean, consumerRecord.value(), message, consumer);
		}

	}

	/*
	 * public void convert(final ConsumerRecord<String, String> consumerRecord) {
	 * 
	 * conv.toMessage(consumerRecord, acknowledgment, consumer, payloadType); }
	 */
}
