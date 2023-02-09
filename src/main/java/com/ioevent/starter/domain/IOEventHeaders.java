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




package com.ioevent.starter.domain;





/**
* enum which describe IOEvent custom Headers for events,
**/
public enum IOEventHeaders {
	CORRELATION_ID,STEP_NAME,INPUT,EVENT_TYPE,OUTPUT_EVENT,START_TIME,API_KEY,PROCESS_NAME,START_INSTANCE_TIME,IMPLICIT_START,IMPLICIT_END,
	ERROR_TYPE,ERROR_MESSAGE,ERROR_TRACE,RESUME

}
