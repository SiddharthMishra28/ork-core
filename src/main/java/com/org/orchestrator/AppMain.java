package com.org.orchestrator;

import com.org.orchestrator.executor.CsvFlowWorker;
import com.org.orchestrator.gitlab.GitLabClient;
import com.org.orchestrator.model.ExecutionSummary;
import com.org.orchestrator.report.HtmlReportGenerator;
import com.org.orchestrator.util.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AppMain {
    private static final Logger log = LoggerFactory.getLogger(AppMain.class);

    public static void main(String[] args) {
        try {
            Config cfg = Config.fromArgs(args);
            List<Path> csvFiles = cfg.getInputCsvs();
            int threads = Math.min(cfg.getMaxThreads(), csvFiles.size());
            if (threads <= 0) {
                threads = 1;
            }

            log.info("Initializing orchestrator with {} threads for {} CSV files.", threads, csvFiles.size());

            ExecutorService executor = Executors.newFixedThreadPool(threads);
            GitLabClient gitlab = new GitLabClient(cfg.getGitlabBaseUrl());

            List<CompletableFuture<ExecutionSummary>> futures = csvFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(() ->
                            new CsvFlowWorker(file, gitlab, cfg).get(), executor))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("All CSV flows have completed. Generating final report.");

            List<ExecutionSummary> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            HtmlReportGenerator.generate(results, Paths.get("execution-summary-" + System.currentTimeMillis() + ".html"));
            executor.shutdown();
            log.info("Orchestration finished.");
        } catch (Exception e) {
            log.error("A fatal error occurred in the application's main thread.", e);
            System.exit(1);
        }
    }
}
