package com.org.orchestrator.model;

public class PipelineStatusInfo {
    private final long pipelineId;
    private final String result;

    public PipelineStatusInfo(long pipelineId, String result) {
        this.pipelineId = pipelineId;
        this.result = result;
    }

    public long getPipelineId() {
        return pipelineId;
    }

    public String getResult() {
        return result;
    }
}
