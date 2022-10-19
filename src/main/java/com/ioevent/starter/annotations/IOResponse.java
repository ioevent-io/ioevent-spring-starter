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



package com.ioevent.starter.annotations;







import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * IOEvent Response annotation allows us to simplify the IOEvent response ,
 **/
@Data
@Builder
@AllArgsConstructor
public class IOResponse<T> {

	private String key;
	private T body;
	private Map<String, Object> headers = new HashMap<>();

	public IOResponse(String key, T body) {
		this.body = body;
		this.key = key;

	}
	public IOResponse(T body,Map<String, Object> headers) {
		this.body = body;
		this.headers = headers;

	}
}
