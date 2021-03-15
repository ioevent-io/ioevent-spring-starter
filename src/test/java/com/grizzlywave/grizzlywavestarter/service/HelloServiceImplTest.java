package com.grizzlywave.grizzlywavestarter.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * test for our auto-configuration service that we had created "HelloService"
 **/
class HelloServiceImplTest {

	@InjectMocks
	HelloService helloservice = new HelloServiceImpl();

	@BeforeEach
	public void init() {
		MockitoAnnotations.initMocks(this);

	}

	@Test
	void testHello() {
		assertEquals("starter msg sent", helloservice.Hello());
	}

}
