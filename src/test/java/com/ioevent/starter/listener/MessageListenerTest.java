package com.ioevent.starter.listener;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import com.ioevent.starter.domain.IOEventMessageEventInformation;

public class MessageListenerTest {
	
	
	@InjectMocks
	MessageListener messageListener = new MessageListener();
	
	
	@Test
	void isValidMessageTest()
	{
		IOEventMessageEventInformation iOEventMessageEventInformation = new IOEventMessageEventInformation();
		List<String> required = Arrays.asList("exemple1","exemple2","exemple3");
		List<String> arrived = Arrays.asList("exemple2","exemple3","exemple1");
		String messageArrived = "messageExample";
		String messageRequired = "messageExample";
		String messageNonRequired = "messageNonExample";
		iOEventMessageEventInformation.setInputsArrived(arrived);
		iOEventMessageEventInformation.setInputRequired(required);
		iOEventMessageEventInformation.setMessageEventRequired(messageRequired);
		iOEventMessageEventInformation.setMessageEventArrived(messageNonRequired);
		Assert.assertFalse(messageListener.validMessage(iOEventMessageEventInformation));
		iOEventMessageEventInformation.setMessageEventArrived(messageArrived);
		Assert.assertTrue(messageListener.validMessage(iOEventMessageEventInformation));
	
	
	}
}
