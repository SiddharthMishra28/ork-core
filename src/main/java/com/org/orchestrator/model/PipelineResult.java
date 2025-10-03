package com.org.orchestrator.model;

import java.util.Map;

public class PipelineResult {
    private final long pipelineId;
    private final String status;
    private final Map<String, String> mergedVars;

    public PipelineResult(long pipelineId, String status, Map<String, String> mergedVars) {
        this.pipelineId = pipelineId;
        this.status = status;
        this.mergedVars = mergedVars;
    }

    public long getPipelineId() {
        return pipelineId;
    }

    public String getStatus() {
        return status;
    }

    public Map<String, String> getMergedVars() {
        return mergedVars;
    }
}
