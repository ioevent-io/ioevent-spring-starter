package com.grizzlywave.starter.configuration.properties;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Class for wave properties that can be specified in the properties file : 
 * - topic_names : list of topics that the user want to create,
 * - prefix : prefix for the topics default "Wave-", 
 * - auto_create_topic : create the topics used in code automatically if true.
 **/
@Configuration
@ConfigurationProperties(prefix = "grizzly-wave")
public class WaveProperties {

	private List<String> topic_names;

	private String prefix = "Wave-";

	private Boolean auto_create_topic = true;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix + "-";
	}

	public List<String> getTopic_names() {
		return topic_names;
	}

	public void setTopic_names(List<String> topic_names) {
		this.topic_names = topic_names;
	}

	public Boolean getAuto_create_topic() {
		return auto_create_topic;
	}

	public void setAuto_create_topic(Boolean auto_create_topic) {
		this.auto_create_topic = auto_create_topic;
	}

	public void logProp() {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

		LOGGER.info("prp" + this.topic_names);
	}

	// standard getters and setters
}
