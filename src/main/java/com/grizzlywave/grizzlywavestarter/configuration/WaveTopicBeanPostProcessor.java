package com.grizzlywave.grizzlywavestarter.configuration;

import java.lang.reflect.Method;
import java.util.Arrays;
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
import org.springframework.lang.Nullable;

import com.grizzlywave.grizzlywavestarter.GrizzlyWaveStarterApplication;
import com.grizzlywave.grizzlywavestarter.annotations.WaveInit;
import com.grizzlywave.grizzlywavestarter.annotations.WaveTransition;
import com.grizzlywave.grizzlywavestarter.service.TopicServices;

/**
 * Class Configuration for Wave Topic creation using Bean Post Processor ,
 * create topics mentioned in property Grizzly-wave topic-names , if the auto
 * creation property is enable : this configuration will create topics mentioned
 * in all annotations if not it will generate an exception
 **/
@Primary
@Configuration
public class WaveTopicBeanPostProcessor implements DestructionAwareBeanPostProcessor,WavePostProcessors {

	private static final Logger log = LoggerFactory.getLogger(GrizzlyWaveStarterApplication.class);

	@Autowired
	WaveConfigProperties waveProperties;

	@Autowired
	private ApplicationContext context;

	@Autowired
	AdminClient client;

	/** BeanPostProcessor method to execute Before Bean Initialization */

	@Nullable
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

		try {
			this.process(bean);
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
			waveProperties.getTopic_names().stream()
					.forEach(x -> ((TopicServices) bean).createTopic(x, waveProperties.getPrefix()));
			log.info("topics created");
		}

		return bean;
	}

	/**
	 * Process Method take the Bean as a parameter collect all Grizzly_Wave custom
	 * annotations Verifies if the topics uses in these annotations are already
	 * exist if not then create all them in condition that the property
	 * auto_create_topic is true
	 **/
	@Override
	public void process(Object bean) throws Exception {
		for (Method method : bean.getClass().getMethods()) {
			WaveInit[] waveInit = method.getAnnotationsByType(WaveInit.class);
			WaveTransition[] waveTransition = method.getAnnotationsByType(WaveTransition.class);

			if (waveInit != null)
				for (WaveInit x : waveInit) {

					if (client.listTopics().names().get().stream().anyMatch(
							topicName -> topicName.equalsIgnoreCase(waveProperties.getPrefix() + x.target_topic())))
						log.info("topic alreay exist");

					else {
						if (waveProperties.getAuto_create_topic()) {
							log.info("creating topic : " + x.target_topic());
							client.createTopics(Arrays
									.asList(new NewTopic(waveProperties.getPrefix() + x.target_topic(), 1, (short) 1)));
						} else
							throw new Exception(
									"Topics doesn't Exist : You must Create them By Adding topics Name in Properties");
					}
				}

			if (waveTransition != null)
				for (WaveTransition x : waveTransition) {

					if ((client.listTopics().names().get().stream().anyMatch(
							topicName -> topicName.equalsIgnoreCase(waveProperties.getPrefix() + x.target_topic())))
							&& (client.listTopics().names().get().stream().anyMatch(topicName -> topicName
									.equalsIgnoreCase(waveProperties.getPrefix() + x.source_topic()))))
						log.info("topics alreay exist");

					else {
						if (waveProperties.getAuto_create_topic()) {
							log.info("creating topic : " + x.target_topic() + " , " + x.source_topic());
							client.createTopics(Arrays
									.asList(new NewTopic(waveProperties.getPrefix() + x.source_topic(), 1, (short) 1)));
							client.createTopics(Arrays
									.asList(new NewTopic(waveProperties.getPrefix() + x.target_topic(), 1, (short) 1)));

						} else
							throw new Exception(
									"Topics doesn't Exist : You must Create them By Adding topics Name in Properties");

					}
				}

		}

	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		// TODO Auto-generated method stub

	}
}
