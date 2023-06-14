package com.ioevent.starter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ioevent.starter.enums.MessageTypesEnum;

@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface IOMessage {
	
	String key() default "";
	
	MessageTypesEnum messageType() default MessageTypesEnum.THROW;

}
