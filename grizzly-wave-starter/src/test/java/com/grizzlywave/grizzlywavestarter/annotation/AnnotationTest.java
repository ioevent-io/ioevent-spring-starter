package com.grizzlywave.grizzlywavestarter.annotation;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * test for our annotation call 
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
public class AnnotationTest {
    @Autowired
    private CustomAnnotationTestDelegate customAnnotationTestDelegate;
    @Test
    public void shouldLogAnnotationMethode() {
       customAnnotationTestDelegate.methodWithCustomAnnotation();
       
    }
}