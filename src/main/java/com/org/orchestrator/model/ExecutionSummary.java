package com.org.orchestrator.model;

import java.util.ArrayList;
import java.util.List;

public class ExecutionSummary {
    private final String csvName;
    private final List<PipelineResult> pipelineResults = new ArrayList<>();
    private String status = "PASSED"; // Default to PASSED

    public ExecutionSummary(String csvName) {
        this.csvName = csvName;
    }

    public void addPipelineResult(PipelineResult result) {
        pipelineResults.add(result);
        if (!"success".equalsIgnoreCase(result.getStatus())) {
            this.status = "FAILED";
        }
    }

    public String getCsvName() {
        return csvName;
    }

    public List<PipelineResult> getPipelineResults() {
        return pipelineResults;
    }

    public String getStatus() {
        return status;
    }
}
