package com.grizzlywave.starter.listener;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.grizzlywave.starter.domain.ParallelEventInfo;

class WaveParrallelListenerTest {

	@InjectMocks
	WaveParrallelListener waveParrallelListener=new WaveParrallelListener();

	Gson gson = new Gson();
	@Test
	void parseConsumedValueTest() throws JsonMappingException, JsonProcessingException {
		// test String object
		Object string = "test String";
		assertEquals(String.class, waveParrallelListener.parseConsumedValue(string, String.class).getClass());
		// test custom object
		ParallelEventInfo parallelEventInfo = new ParallelEventInfo("id",
				Arrays.asList("first element ", "second element"));
		Object parallelString = gson.toJson(parallelEventInfo);
		assertEquals(ParallelEventInfo.class,
				waveParrallelListener.parseConsumedValue(parallelString, ParallelEventInfo.class).getClass());

	}
	@Test
	 void sameListTest() {
		List<String> l1 = Arrays.asList("1","2","5");
		List<String> l2 = Arrays.asList("1","2","5");
		List<String> l3 = Arrays.asList("1","2");
		List<String> l4 = Arrays.asList("1","5");
		Assert.assertEquals(waveParrallelListener.sameList(l1, l2), true);
		Assert.assertEquals(waveParrallelListener.sameList(l3, l1), false);
		Assert.assertEquals(waveParrallelListener.sameList(l3, l4), false);
		Assert.assertEquals(waveParrallelListener.sameList(l4, l3), false);


	}

}
