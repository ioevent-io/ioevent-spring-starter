package com.grizzlywave.grizzlywavestarter.configuration;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Primary
@Configuration
@ConfigurationProperties(prefix = "grizzly-wave")
public class WaveConfigProperties {

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
