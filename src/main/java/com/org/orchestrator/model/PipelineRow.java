package com.org.orchestrator.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PipelineRow {
    private final String applicationName;
    private final long projectId;
    private final String accessToken;
    private final String branch;
    private final String variablesAndValues;
    private final String artifactJobName;
    private final int order;

    private final Map<String, String> staticVars;
    private final Set<String> declaredKeys;

    public PipelineRow(String applicationName, long projectId, String accessToken, String branch,
                       String variablesAndValues, String artifactJobName, int order) {
        this.applicationName = applicationName;
        this.projectId = projectId;
        this.accessToken = accessToken;
        this.branch = branch;
        this.variablesAndValues = variablesAndValues;
        this.artifactJobName = artifactJobName;
        this.order = order;
        this.staticVars = parseVariables(variablesAndValues);
        this.declaredKeys = staticVars.keySet();
    }

    private Map<String, String> parseVariables(String vars) {
        if (vars == null || vars.isBlank()) {
            return Collections.emptyMap();
        }
        vars = vars.trim();
        if (vars.startsWith("{")) { // JSON
            try {
                return new ObjectMapper().readValue(vars, new TypeReference<Map<String, String>>() {});
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON in variablesAndValues column: " + vars, e);
            }
        } else { // Key-value pairs
            return Stream.of(vars.split("[;,]"))
                    .map(String::trim)
                    .filter(s -> s.contains("="))
                    .map(kv -> kv.split("=", 2))
                    .collect(Collectors.toMap(a -> a[0].trim(), a -> a[1].trim(), (v1, v2) -> v2, HashMap::new));
        }
    }

    // Getters
    public String getApplicationName() { return applicationName; }
    public long getProjectId() { return projectId; }
    public String getAccessToken() { return accessToken; }
    public String getBranch() { return branch; }
    public String getVariablesAndValues() { return variablesAndValues; }
    public String getArtifactJobName() { return artifactJobName; }
    public int getOrder() { return order; }
    public Map<String, String> getStaticVars() { return staticVars; }
    public Set<String> getDeclaredKeys() { return declaredKeys; }

    public Map<String, String> getAllVars() {
        Map<String, String> allVars = new HashMap<>();
        allVars.put("applicationName", applicationName);
        allVars.put("projectId", String.valueOf(projectId));
        allVars.put("branch", branch);
        allVars.put("artifactJobName", artifactJobName);
        allVars.put("order", String.valueOf(order));
        allVars.putAll(staticVars);
        return allVars;
    }
}
