package com.grizzlywave.grizzlywavestarter.configuration;

public interface WavePostProcessors {

	void process(Object bean, String workFlow) throws Exception;

}
