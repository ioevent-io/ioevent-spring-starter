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






import com.ioevent.starter.handler.IOEventRecordInfo;

public class IOEventContextHolder {

	private static ThreadLocal<IOEventRecordInfo> eventContextHolder = new ThreadLocal<>();

	public static void setContext(IOEventRecordInfo ioeventRecordInfo) {
		eventContextHolder.set(ioeventRecordInfo);
	}

	public static IOEventRecordInfo getContext() {
		return eventContextHolder.get();
	}

	public static void unload() {
		eventContextHolder.remove();
	}

}
