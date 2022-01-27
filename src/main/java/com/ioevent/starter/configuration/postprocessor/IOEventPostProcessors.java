package com.ioevent.starter.configuration.postprocessor;

public interface IOEventPostProcessors {

	void process(Object bean, String workFlow) throws Exception, Throwable;

}
