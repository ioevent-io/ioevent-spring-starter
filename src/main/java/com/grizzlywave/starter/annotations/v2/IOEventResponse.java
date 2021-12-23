package com.grizzlywave.starter.annotations.v2;

import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * IOEvent annotation allows us to simplify the IOEvent response ,
 **/
@Data
@AllArgsConstructor
public class IOEventResponse<T> {

	private String string;
	private T body;

}
