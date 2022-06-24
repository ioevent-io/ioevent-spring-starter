/*
 * Copyright © 2021 CodeOnce Software (https://www.codeonce.fr/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




package com.ioevent.starter.service;






import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

import com.ioevent.starter.handler.IOEventRecordInfo;

class IOEventContextHolderTest {

	@Test
	void ioeventContextTest() {
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("object stored in thread local", "name", "output", new StopWatch(),1000L);
		IOEventContextHolder.setContext(ioeventRecordInfo);
		Assert.assertEquals(ioeventRecordInfo, IOEventContextHolder.getContext());
	}
	@Test
	void ioeventContextUnloadTest() {
		IOEventRecordInfo ioeventRecordInfo = new IOEventRecordInfo("object stored in thread local", "name", "output", new StopWatch(),1000L);
		IOEventContextHolder.setContext(ioeventRecordInfo);
		Assert.assertEquals(ioeventRecordInfo, IOEventContextHolder.getContext());
		IOEventContextHolder.unload();
		assertNull(IOEventContextHolder.getContext());}

}
