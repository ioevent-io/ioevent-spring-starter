package com.ioevent.starter.annotations;

public class IOHumanInfos {
    private Object originalPayload;
    private Object humanResponse;

    public IOHumanInfos(Object originalPayload, Object humanResponse) {
        this.originalPayload = originalPayload;
        this.humanResponse = humanResponse;
    }

    public Object getOriginalPayload() {
        return originalPayload;
    }

    public void setOriginalPayload(Object originalPayload) {
        this.originalPayload = originalPayload;
    }

    public Object getHumanResponse() {
        return humanResponse;
    }

    public void setHumanResponse(Object humanResponse) {
        this.humanResponse = humanResponse;
    }

    @Override
    public String toString() {
        return "IOHumanInfos{" +
                "originalPayload=" + originalPayload +
                ", humanResponse=" + humanResponse +
                '}';
    }
}
