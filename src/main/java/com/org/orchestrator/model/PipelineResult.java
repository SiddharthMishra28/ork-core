package com.org.orchestrator.model;

import java.util.Map;

public class PipelineResult {
    private final long pipelineId;
    private final String status;
    private final Map<String, String> mergedVars;
    private final Map<String, String> rowVars;

    public PipelineResult(long pipelineId, String status, Map<String, String> mergedVars, Map<String, String> rowVars) {
        this.pipelineId = pipelineId;
        this.status = status;
        this.mergedVars = mergedVars;
        this.rowVars = rowVars;
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

    public Map<String, String> getRowVars() {
        return rowVars;
    }
}
