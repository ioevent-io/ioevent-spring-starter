package com.grizzlywave.grizzlywavestarter.annotation;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.grizzlywave.starter.annotations.EnableGrizzlyWave;

/**
 * test for our annotations call 
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@EnableGrizzlyWave
public class AnnotationTest {


    /**
     *call of @waveInit Annotation 
     **/
    @Test
    public void shouldLogWaveInitAnnotationMethod() {
    	System.out.println("starter test");
    }
  
}