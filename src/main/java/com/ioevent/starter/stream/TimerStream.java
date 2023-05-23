package com.ioevent.starter.stream;

import java.util.Date;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.google.gson.Gson;
import com.ioevent.starter.domain.IOTimerEvent;
import com.ioevent.starter.service.IOEventMessageBuilderService;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class TimerStream {

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	IOEventMessageBuilderService ioeventMessageBuilderService;
	
	/**
	 * method for processing Start-Timer events from the
	 * ioevent-timer topic using kafka stream,
	 * 
	 * @param builder type of StreamsBuilder,
	 */
	@Autowired
	public void processTimer(final StreamsBuilder builder) {

		Gson gson = new Gson();

		KStream<String, String> kstream = builder
				.stream("ioevent-timer", Consumed.with(Serdes.String(), Serdes.String())).map(KeyValue::new)
				.filter((k, v) -> {
					IOTimerEvent value = gson.fromJson(v, IOTimerEvent.class);
					return appName.equals(value.getAppName());
				});

		kstream.groupByKey(Grouped.with(Serdes.String(), Serdes.String())).reduce((aggValue, newValue) -> {
			if (aggValue == null) {
				return newValue;
			}
			if (checkCron(newValue, aggValue)) {
				IOTimerEvent newTimerEvent = gson.fromJson(newValue, IOTimerEvent.class);
				ioeventMessageBuilderService.sendTimerEvent(newTimerEvent, "ioevent-timer-execute");
				return newValue;
			}

			return aggValue;
		});

		
	}

	private boolean checkCron(String newValue, String aggValue) {
		Gson gson = new Gson();
		IOTimerEvent newTimerEvent = gson.fromJson(newValue, IOTimerEvent.class);
		IOTimerEvent aggTimerEvent = gson.fromJson(aggValue, IOTimerEvent.class);
		Date lastTimerExecution = new Date(aggTimerEvent.getTime());
		Date newTimerExecution = new Date(newTimerEvent.getTime());
		CronTrigger cronTrigger = new CronTrigger(newTimerEvent.getCron());
		TriggerContext triggerContext = new SimpleTriggerContext(lastTimerExecution, lastTimerExecution,
				lastTimerExecution);
		Date nextDate = cronTrigger.nextExecutionTime(triggerContext);
		return newTimerExecution.after(nextDate);
	}
}
