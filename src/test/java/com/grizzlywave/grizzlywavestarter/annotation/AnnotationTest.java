package com.grizzlywave.grizzlywavestarter.annotation;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.grizzlywave.grizzlywavestarter.model.Order;

/**
 * test for our annotations call 
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
public class AnnotationTest {
    @Autowired
    CustomAnnotationTestDelegate customAnnotationTestDelegate;
	@Autowired
    WaveInitAnnotation waveInitAnnotation;
//	@Autowired
//	WaveTransitionAnnotation waveTransitionAnnotation;
/**
 * call of our first annotation example
 * */
	@Test
    public void shouldLogFirstAnnotationMethod() {
       customAnnotationTestDelegate.methodWithCustomAnnotation();
    }
    /**
     *call of @waveInit Annotation 
     **/
    @Test
    public void shouldLogWaveInitAnnotationMethod() {
    	
    	waveInitAnnotation.initOrder(new Order(2, 5, 200));
    }
    /**
     *call of @waveInit Annotation 
     **/
//    @Test
//    public void shouldLogWaveTransitionAnnotationMethod() {
//    	
//    	waveTransitionAnnotation.tryWveTransition(new Order(2, 5, 200));
//    }
}