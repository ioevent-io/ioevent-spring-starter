package com.ioevent.starter.service;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import com.ioevent.starter.handler.IOEventRecordInfo;
import com.ioevent.starter.service.IOEventContextHolder;

class IOEventContextHolderTest {

	@Test
	void ioeventContextTest() {
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("object stored in thread local", "name", "target", new StopWatch());
		IOEventContextHolder.setContext(ioeventRecordInfo);
		Assert.assertEquals(ioeventRecordInfo, IOEventContextHolder.getContext());
	}
	@Test
	void ioeventContextUnloadTest() {
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("object stored in thread local", "name", "target", new StopWatch());
		IOEventContextHolder.setContext(ioeventRecordInfo);
		Assert.assertEquals(ioeventRecordInfo, IOEventContextHolder.getContext());
		IOEventContextHolder.unload();
		assertNull(IOEventContextHolder.getContext());}

}
