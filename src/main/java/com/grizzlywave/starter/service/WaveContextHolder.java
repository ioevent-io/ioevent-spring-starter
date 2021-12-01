package com.grizzlywave.starter.service;

import com.grizzlywave.starter.handler.WaveRecordInfo;

public class WaveContextHolder {

	private static ThreadLocal<WaveRecordInfo> eventContextHolder = new ThreadLocal<>();

	public static void setContext(WaveRecordInfo waveRecordInfo) {
		eventContextHolder.set(waveRecordInfo);
	}

	public static WaveRecordInfo getContext() {
		return eventContextHolder.get();
	}

	public static void unload() {
		eventContextHolder.remove();
	}

}
