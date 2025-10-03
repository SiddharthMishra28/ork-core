package com.org.orchestrator.gitlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.orchestrator.model.JobInfo;
import com.org.orchestrator.model.PipelineStatusInfo;
import com.org.orchestrator.util.OutputEnvParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GitLabClient {
    private static final Logger log = LoggerFactory.getLogger(GitLabClient.class);
    private final String baseUrl;
    private final CloseableHttpClient http;
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public GitLabClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClients.createDefault();
    }

    public long triggerPipeline(long projectId, String branch, Map<String, String> vars, String token) {
        String url = baseUrl + "/projects/" + projectId + "/pipeline";
        try {
            HttpPost post = new HttpPost(url);
            post.setHeader("PRIVATE-TOKEN", token);
            post.setHeader("Content-Type", "application/json");

            StringBuilder jsonPayload = new StringBuilder();
            jsonPayload.append("{\"ref\":\"").append(branch).append("\", \"variables\":[");
            vars.forEach((k, v) -> jsonPayload.append("{\"key\":\"").append(k).append("\",\"value\":\"").append(v).append("\"},"));
            if (!vars.isEmpty()) {
                jsonPayload.setLength(jsonPayload.length() - 1); // Remove last comma
            }
            jsonPayload.append("]}");

            post.setEntity(new StringEntity(jsonPayload.toString()));

            return http.execute(post, response -> {
                if (response.getCode() != 201) {
                    throw new IOException("Failed to trigger pipeline: " + response.getCode() + " " + EntityUtils.toString(response.getEntity()));
                }
                JsonNode root = jsonMapper.readTree(response.getEntity().getContent());
                long pipelineId = root.get("id").asLong();
                log.info("Triggered pipeline with ID: {} for project {}", pipelineId, projectId);
                return pipelineId;
            });
        } catch (Exception e) {
            log.error("Error triggering pipeline for project {}: {}", projectId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public PipelineStatusInfo pollPipeline(long projectId, long pipelineId, String token, int pollInterval) {
        String url = baseUrl + "/projects/" + projectId + "/pipelines/" + pipelineId;
        HttpGet get = new HttpGet(url);
        get.setHeader("PRIVATE-TOKEN", token);

        while (true) {
            try {
                String status = http.execute(get, response -> {
                    if (response.getCode() != 200) {
                        throw new IOException("Failed to poll pipeline: " + response.getCode() + " " + EntityUtils.toString(response.getEntity()));
                    }
                    JsonNode root = jsonMapper.readTree(response.getEntity().getContent());
                    return root.get("status").asText();
                });

                log.info("Polling pipeline {} for project {}. Current status: {}", pipelineId, projectId, status);
                if ("success".equalsIgnoreCase(status) || "failed".equalsIgnoreCase(status) || "canceled".equalsIgnoreCase(status)) {
                    return new PipelineStatusInfo(pipelineId, status);
                }
                Thread.sleep(pollInterval * 1000L);
            } catch (Exception e) {
                log.error("Error polling pipeline {} for project {}: {}", pipelineId, projectId, e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    public Map<String, String> fetchOutputEnv(long projectId, long pipelineId, String artifactJobName, String accessToken, String artifactPath) throws IOException {
        log.info("Attempting to fetch artifact '{}' for job '{}' in pipeline {} (project {})", artifactPath, artifactJobName, pipelineId, projectId);
        long jobId = -1;

        try {
            List<JobInfo> jobs = listJobs(projectId, pipelineId, accessToken);
            Optional<JobInfo> targetJob = pickJobWithArtifacts(jobs, artifactJobName);
            if (targetJob.isEmpty()) {
                log.warn("No suitable job with artifacts found for pipeline {} with job name {}", pipelineId, artifactJobName);
                return Collections.emptyMap();
            }
            jobId = targetJob.get().getId();

            String url = String.format("%s/projects/%d/jobs/%d/artifacts/raw/%s", baseUrl, projectId, jobId, artifactPath);
            HttpGet get = new HttpGet(url);
            get.setHeader("PRIVATE-TOKEN", accessToken);

            return http.execute(get, response -> {
                if (response.getCode() != 200) {
                    log.warn("Failed to download artifact '{}' for job {}. Status: {}", artifactPath, jobId, response.getCode());
                    return Collections.emptyMap();
                }

                try (java.io.InputStream content = response.getEntity().getContent()) {
                    log.info("Found artifact '{}' for job {}. Parsing.", artifactPath, jobId);
                    return OutputEnvParser.parse(content);
                } catch (Exception e) {
                    log.error("Failed to process artifact stream for job {}. Error: {}", jobId, e.getMessage());
                    return Collections.emptyMap();
                }
            });
        } catch (Exception e) {
            log.error("A critical error occurred during artifact fetching for pipeline {}: {}", pipelineId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<JobInfo> listJobs(long projectId, long pipelineId, String token) throws IOException {
        String url = baseUrl + "/projects/" + projectId + "/pipelines/" + pipelineId + "/jobs";
        HttpGet get = new HttpGet(url);
        get.setHeader("PRIVATE-TOKEN", token);

        return http.execute(get, response -> {
            if (response.getCode() != 200) {
                throw new IOException("Failed to list jobs: " + response.getCode() + " " + EntityUtils.toString(response.getEntity()));
            }
            List<JobInfo> jobs = new ArrayList<>();
            JsonNode root = jsonMapper.readTree(response.getEntity().getContent());
            if (root.isArray()) {
                for (JsonNode jobNode : root) {
                    boolean hasArtifacts = jobNode.has("artifacts") && jobNode.get("artifacts").isArray() && !jobNode.get("artifacts").isEmpty();
                    jobs.add(new JobInfo(jobNode.get("id").asLong(), jobNode.get("name").asText(), hasArtifacts));
                }
            }
            return jobs;
        });
    }

    private Optional<JobInfo> pickJobWithArtifacts(List<JobInfo> jobs, String jobName) {
        java.util.stream.Stream<JobInfo> jobStream = jobs.stream();

        // If a specific job name is given, filter by it first.
        if (jobName != null && !jobName.isBlank()) {
            jobStream = jobStream.filter(j -> jobName.equals(j.getName()));
        }

        // From the filtered stream (or the whole stream), find the first one with artifacts.
        return jobStream.filter(JobInfo::getHaveArtifacts).findFirst();
    }
}
