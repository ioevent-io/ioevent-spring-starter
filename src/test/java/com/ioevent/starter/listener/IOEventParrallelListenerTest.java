package com.ioevent.starter.listener;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.ioevent.starter.domain.ParallelEventInfo;
import com.ioevent.starter.listener.IOEventParrallelListener;

class IOEventParrallelListenerTest {

	@InjectMocks
	IOEventParrallelListener ioeventParrallelListener=new IOEventParrallelListener();

	Gson gson = new Gson();
	@Test
	void parseConsumedValueTest() throws JsonMappingException, JsonProcessingException {
		// test String object
		Object string = "test String";
		assertEquals(String.class, ioeventParrallelListener.parseConsumedValue(string, String.class).getClass());
		// test custom object
		ParallelEventInfo parallelEventInfo = new ParallelEventInfo("id",
				Arrays.asList("first element ", "second element"));
		Object parallelString = gson.toJson(parallelEventInfo);
		assertEquals(ParallelEventInfo.class,
				ioeventParrallelListener.parseConsumedValue(parallelString, ParallelEventInfo.class).getClass());

	}
	@Test
	 void sameListTest() {
		List<String> l1 = Arrays.asList("1","2","5");
		List<String> l2 = Arrays.asList("1","2","5");
		List<String> l3 = Arrays.asList("1","2");
		List<String> l4 = Arrays.asList("1","5");
		Assert.assertTrue(ioeventParrallelListener.sameList(l1, l2));
		Assert.assertFalse(ioeventParrallelListener.sameList(l3, l1));
		Assert.assertFalse(ioeventParrallelListener.sameList(l3, l4));
		Assert.assertFalse(ioeventParrallelListener.sameList(l4, l3));


	}

}
