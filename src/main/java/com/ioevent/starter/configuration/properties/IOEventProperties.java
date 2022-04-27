package com.ioevent.starter.configuration.properties;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Class for ioevent properties that can be specified in the properties file : 
 * - topic_names : list of topics that the user want to create,
 * - prefix : prefix for the topics default "IOEvent-", 
 * - group_id : group id for kafka consumer,
 * - auto_create_topic : create the topics used in code automatically if true.
 **/
@Configuration
@ConfigurationProperties(prefix = "ioevent")
public class IOEventProperties {

	

	private List<String> topic_names;

	private String prefix = "IOEvent-";
	
	private String group_id= "ioevent";
	private Boolean auto_create_topic = true;
	private String topic_replication="1" ;
	private String api_key = "";
	private int topic_partition=1;
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

	
	public String getTopicReplication() {
		return topic_replication;
	}

	public void setTopicReplication(String topicReplication) {
		this.topic_replication = topicReplication;
	}

	public String getGroup_id() {
		return group_id;
	}

	public void setGroup_id(String group_id) {
		this.group_id = group_id;
	}
	
	public Boolean getAuto_create_topic() {
		return auto_create_topic;
	}

	public void setAuto_create_topic(Boolean auto_create_topic) {
		this.auto_create_topic = auto_create_topic;
	}

	public String getApikey() {
		return api_key;
	}

	public int getTopic_partition() {
		return topic_partition;
	}

	public void setTopic_partition(int topic_partition) {
		this.topic_partition = topic_partition;
	}

	public void setApikey(String api_key) {
		this.api_key = api_key;
	}

	public void logProp() {
		Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

		LOGGER.info("prp" + this.topic_names);
	}

	// standard getters and setters
}
