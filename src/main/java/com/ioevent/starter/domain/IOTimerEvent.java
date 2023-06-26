package com.ioevent.starter.domain;

public class IOTimerEvent {
	
	String cron;
	String methodName;
	String methodeQialifiedName;
	String bean;
	String appName;
	Long time ;
	
	public IOTimerEvent() {
		super();
	}
	

	public IOTimerEvent(String cron, String methodName,String methodeQialifiedName, String beanName,String appName,Long time) {
		super();
		this.cron = cron;
		this.methodName = methodName;
		this.methodeQialifiedName=methodeQialifiedName;
		this.bean = beanName;	
		this.appName=appName;
		this.time=time;
		}

	public String getCron() {
		return cron;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public String getMethodName() {
		return methodName;
	}


	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}


	public String getMethodeQialifiedName() {
		return methodeQialifiedName;
	}


	public void setMethodeQialifiedName(String methodeQialifiedName) {
		this.methodeQialifiedName = methodeQialifiedName;
	}


	public String getBean() {
		return bean;
	}

	public void setBean(String bean) {
		this.bean = bean;
	}


	public String getAppName() {
		return appName;
	}


	public void setAppName(String appName) {
		this.appName = appName;
	}


	public Long getTime() {
		return time;
	}


	public void setTime(Long time) {
		this.time = time;
	}


	@Override
	public String toString() {
		return "IOTimerEvent [cron=" + cron + ", methodName=" + methodName + ", methodeQialifiedName="
				+ methodeQialifiedName + ", bean=" + bean + ", appName=" + appName + ", time=" + time + "]";
	}


	


	
	

}
