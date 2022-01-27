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
