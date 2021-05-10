package com.grizzlywave.starter.configuration.postprocessor;

public interface WavePostProcessors {

	void process(Object bean, String workFlow) throws Exception, Throwable;

}
