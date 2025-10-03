package com.org.orchestrator.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Config {
    private List<Path> inputCsvs;
    private int maxThreads = 10;
    private int pollInterval = 10; // seconds
    private String gitlabBaseUrl = "https://gitlab.com/api/v4";

    private Config() {}

    public static Config fromArgs(String[] args) {
        Config config = new Config();
        for (String arg : args) {
            if (arg.startsWith("--input=")) {
                String csvs = arg.substring("--input=".length());
                config.inputCsvs = Arrays.stream(csvs.split(","))
                                           .map(String::trim)
                                           .map(Paths::get)
                                           .collect(Collectors.toList());
            } else if (arg.startsWith("--maxThreads=")) {
                config.maxThreads = Integer.parseInt(arg.substring("--maxThreads=".length()));
            } else if (arg.startsWith("--pollInterval=")) {
                config.pollInterval = Integer.parseInt(arg.substring("--pollInterval=".length()));
            } else if (arg.startsWith("--gitlabBaseUrl=")) {
                config.gitlabBaseUrl = arg.substring("--gitlabBaseUrl=".length());
            }
        }

        if (config.inputCsvs == null || config.inputCsvs.isEmpty()) {
            throw new IllegalArgumentException("Missing required argument: --input=\"file1.csv,file2.csv\"");
        }

        return config;
    }

    // Getters
    public List<Path> getInputCsvs() { return inputCsvs; }
    public int getMaxThreads() { return maxThreads; }
    public int getPollInterval() { return pollInterval; }
    public String getGitlabBaseUrl() { return gitlabBaseUrl; }
}
