package com.org.orchestrator.executor;

import com.org.orchestrator.csv.FlowCsvReader;
import com.org.orchestrator.gitlab.GitLabClient;
import com.org.orchestrator.model.ExecutionSummary;
import com.org.orchestrator.model.PipelineResult;
import com.org.orchestrator.model.PipelineRow;
import com.org.orchestrator.model.PipelineStatusInfo;
import com.org.orchestrator.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.org.orchestrator.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CsvFlowWorker implements Supplier<ExecutionSummary> {
    private static final Logger log = LoggerFactory.getLogger(CsvFlowWorker.class);
    private final Path csvPath;
    private final GitLabClient gitlab;
    private final Config cfg;

    public CsvFlowWorker(Path csvPath, GitLabClient gitlab, Config cfg) {
        this.csvPath = csvPath;
        this.gitlab = gitlab;
        this.cfg = cfg;
    }

    @Override
    public ExecutionSummary get() {
        log.info("Starting processing of CSV file: {}", csvPath);
        List<PipelineRow> rows = FlowCsvReader.read(csvPath);
        Map<String, String> runtimeVars = new HashMap<>();
        ExecutionSummary summary = new ExecutionSummary(csvPath.getFileName().toString());

        for (PipelineRow row : rows) {
            try {
                Map<String, String> triggerVars = mergeRowVars(row, runtimeVars);
                log.info("Triggering pipeline for '{}' on branch '{}' with vars: {}", row.getApplicationName(), row.getBranch(), triggerVars);

                long pipelineId = gitlab.triggerPipeline(row.getProjectId(), row.getBranch(), triggerVars, row.getAccessToken());
                PipelineStatusInfo status = gitlab.pollPipeline(row.getProjectId(), pipelineId, row.getAccessToken(), cfg.getPollInterval());

                log.info("Pipeline {} for '{}' finished with status: {}", pipelineId, row.getApplicationName(), status.getResult());

                Map<String, String> parsed = gitlab.fetchOutputEnv(row.getProjectId(), pipelineId, row.getArtifactJobName(), row.getAccessToken(), "target/output.env");
                if (!parsed.isEmpty()) {
                    log.info("Merging runtime vars from pipeline {}: {}", pipelineId, parsed);
                    runtimeVars.putAll(parsed);
                } else {
                    log.warn("No output.env found or parsed for pipeline {}", pipelineId);
                }

                summary.addPipelineResult(new PipelineResult(pipelineId, status.getResult(), new HashMap<>(runtimeVars), row.getAllVars()));

                if (!"success".equalsIgnoreCase(status.getResult())) {
                    log.error("Pipeline {} failed. Halting execution for CSV file: {}", pipelineId, csvPath);
                    break; // Stop processing this CSV if a pipeline fails
                }
            } catch (Exception e) {
                log.error("A critical error occurred while processing a row for {}. Halting flow.", csvPath, e);
                summary.addPipelineResult(new PipelineResult(-1, "ERROR: " + e.getMessage(), new HashMap<>(runtimeVars), row.getAllVars()));
                break;
            }
        }
        log.info("Finished processing CSV file: {}. Final status: {}", csvPath, summary.getStatus());
        return summary;
    }

    private Map<String, String> mergeRowVars(PipelineRow row, Map<String, String> runtime) {
        Map<String, String> vars = new HashMap<>(runtime);
        vars.putAll(row.getStaticVars());
        return vars;
    }
}
