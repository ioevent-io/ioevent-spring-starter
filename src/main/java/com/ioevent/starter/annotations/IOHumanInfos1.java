package com.ioevent.starter.annotations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IOHumanInfos1<T, U> {
    private T originalPayload;
    private U humanResponse;

    @JsonCreator
    public IOHumanInfos1(@JsonProperty("originalPayload") T originalPayload,@JsonProperty("humanResponse") U humanResponse) {
        this.originalPayload = originalPayload;
        this.humanResponse = humanResponse;
    }

    public T getOriginalPayload() {
        return originalPayload;
    }

    public void setOriginalPayload(T originalPayload) {
        this.originalPayload = originalPayload;
    }

    public U getHumanResponse() {
        return humanResponse;
    }

    public void setHumanResponse(U humanResponse) {
        this.humanResponse = humanResponse;
    }
}
