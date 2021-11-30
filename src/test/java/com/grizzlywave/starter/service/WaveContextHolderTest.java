package com.grizzlywave.starter.service;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import com.grizzlywave.starter.handler.WaveRecordInfo;

class WaveContextHolderTest {

	@Test
	void waveContextTest() {
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("object stored in thread local", "name", "target", new StopWatch());
		WaveContextHolder.setContext(waveRecordInfo);
		Assert.assertEquals(waveRecordInfo, WaveContextHolder.getContext());
	}
	@Test
	void waveContextUnloadTest() {
		WaveRecordInfo waveRecordInfo = new WaveRecordInfo("object stored in thread local", "name", "target", new StopWatch());
		WaveContextHolder.setContext(waveRecordInfo);
		Assert.assertEquals(waveRecordInfo, WaveContextHolder.getContext());
		WaveContextHolder.unload();
		assertNull(WaveContextHolder.getContext());}

}
