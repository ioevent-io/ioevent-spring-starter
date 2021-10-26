package com.grizzlywave.starter.configuration.postprocessor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.lang.Nullable;

import com.grizzlywave.starter.GrizzlyWaveStarterApplication;
import com.grizzlywave.starter.annotations.v2.IOEvent;
import com.grizzlywave.starter.configuration.properties.WaveProperties;
import com.grizzlywave.starter.domain.ParallelEventInfo;
import com.grizzlywave.starter.service.IOEventService;
import com.grizzlywave.starter.service.TopicServices;
import io.confluent.ksql.api.client.Client;

/**
 * Class Configuration for Wave Topic creation using Bean Post Processor ,
 * create topics mentioned in property Grizzly-wave topic-names , if the auto
 * creation property is enable : this configuration will create topics mentioned
 * in all annotations if not it will generate an exception
 **/
@Primary
@Configuration
public class WaveTopicBeanPostProcessor implements DestructionAwareBeanPostProcessor, WavePostProcessors {

	private static final Logger log = LoggerFactory.getLogger(GrizzlyWaveStarterApplication.class);

	@Autowired
	private WaveProperties waveProperties;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private AdminClient client;

	@Autowired
	private IOEventService ioEventService;
	
	@Autowired
	private Client KsqlClient;

	/** BeanPostProcessor method to execute Before Bean Initialization */

	@Nullable
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		try {
			this.process(bean, beanName);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			// log.er
		} catch (Exception e) {
			e.printStackTrace();
			SpringApplication.exit(context);
		}
		return bean;
	}

	/** BeanPostProcessor method to execute After Bean Initialization */
	@Nullable
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		
		if (bean instanceof TopicServices) {
			((TopicServices) bean).createTopic("parallelEvent","");
			try {
				createKtable();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			if (waveProperties.getTopic_names()!=null) {
				waveProperties.getTopic_names().stream()
				.forEach(x -> ((TopicServices) bean).createTopic(x, waveProperties.getPrefix()));
		log.info("topics created");
			}
			ioEventService.sendParallelEventInfo(new ParallelEventInfo("first event",Arrays.asList("first target")));

		}
		return bean;
	}

	private void createKtable() throws InterruptedException, ExecutionException {
		String createAccountTable = "CREATE TABLE IF NOT EXISTS parallelEvent (\n"
                + "  id string PRIMARY KEY,\n"
                + "  targets STRING\n"
                + ") WITH (\n"
                + "  KAFKA_TOPIC = 'parallelEvent',\n"
                + "  VALUE_FORMAT='JSON'\n"
                + ");";	
		final Map<String, Object> properties = Map.of("auto.offset.reset", "earliest");
		log.info(KsqlClient.executeStatement(createAccountTable, properties).get().toString());	
		String createQueryableTable = "CREATE TABLE IF NOT EXISTS QUERYABLE_parallelEvent AS SELECT * FROM parallelEvent;";
		log.info(KsqlClient.executeStatement(createQueryableTable, properties).get().toString());	
	}

	/**
	 * Process Method take the Bean as a parameter collect all Grizzly_Wave custom
	 * annotations Verifies if the topics uses in these annotations are already
	 * exist if not then create all them in condition that the property
	 * auto_create_topic is true
	 **/
	@Override
	public void process(Object bean, String beanName) throws Exception {
		for (Method method : bean.getClass().getMethods()) {
			IOEvent[] ioEvents = method.getAnnotationsByType(IOEvent.class);
		 
			if (ioEvents.length != 0) {
				for (IOEvent ioEvent : ioEvents) {
					for (String topicName : ioEventService.getTopics(ioEvent)) {
						if (!topicExist(topicName)) {

							if (waveProperties.getAuto_create_topic()) {
								log.info("creating topic : " + topicName);

								System.out.println("hellooooooooooooooooooooooooooooooooo");
								Thread.sleep(80000);
								TopicBuilder.name(waveProperties.getPrefix()+ topicName).partitions(1).replicas((short) 1).build();
//								client.createTopics(Arrays
//										.asList(new NewTopic(waveProperties.getPrefix() + topicName, 1, (short) 1)));

							} else
								throw new Exception(
										"Topics doesn't Exist : You must Create them By Adding topics Name in Properties");

						}
					}
				}
			}

		}

	}

	private boolean topicExist(String topic) throws InterruptedException, ExecutionException {
		if ((client.listTopics().names().get().stream()
				.anyMatch(topicName -> topicName.equalsIgnoreCase(waveProperties.getPrefix() + topic)))) {
			log.info("topic : "+waveProperties.getPrefix()+topic +"alreay exist");
			return true;
		} else {
			return false;
		}
	}

	
	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		// TODO Auto-generated method stub

	}
}
