package com.grizzlywave.grizzlywavestarter.annotation;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.grizzlywave.grizzlywavestarter.annotations.EnableGrizzlyWave;
import com.grizzlywave.grizzlywavestarter.model.Order;

/**
 * test for our annotations call 
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableGrizzlyWave
public class AnnotationTest {
    @Autowired
    CustomAnnotationTestDelegate customAnnotationTestDelegate;
	@Autowired
    WaveInitAnnotationTest waveInitAnnotation;
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
  
}