package com.grizzlywave.grizzlywavestarter.annotation;

import java.util.logging.Logger;

import org.springframework.stereotype.Component;

import com.grizzlywave.grizzlywavestarter.annotations.firstAnnotation;

/**
 * class that we need for our annotation call test : in this class we call the
 * annotation and in the test class we will use this class
 **/
@Component
public class CustomAnnotationTestDelegate {
	@firstAnnotation
	public void methodWithCustomAnnotation() {
		Logger LOGGER = Logger.getLogger(
			    Thread.currentThread().getStackTrace()[0].getClassName() );
		LOGGER.info("after annotation msg");
	}
}