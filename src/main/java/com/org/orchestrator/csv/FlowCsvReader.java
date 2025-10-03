package com.org.orchestrator.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.org.orchestrator.model.PipelineRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FlowCsvReader {

    private static final Logger log = LoggerFactory.getLogger(FlowCsvReader.class);

    public static List<PipelineRow> read(Path csvPath) {
        List<PipelineRow> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvPath.toFile()))) {
            String[] headers = reader.readNext(); // Read header
            if (headers == null) {
                log.warn("CSV file is empty: {}", csvPath);
                return rows;
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                try {
                    String applicationName = getColumn(line, headers, "applicationName");
                    long projectId = Long.parseLong(getColumn(line, headers, "projectId"));
                    String accessToken = getColumn(line, headers, "accessToken");
                    String branch = getColumn(line, headers, "branch");
                    String variablesAndValues = getColumn(line, headers, "variablesAndValues");
                    String artifactJobName = getColumn(line, headers, "artifactJobName");
                    String orderStr = getColumn(line, headers, "order");
                    int order = (orderStr == null || orderStr.isBlank()) ? Integer.MAX_VALUE : Integer.parseInt(orderStr);

                    rows.add(new PipelineRow(applicationName, projectId, accessToken, branch, variablesAndValues, artifactJobName, order));
                } catch (NumberFormatException e) {
                    log.error("Skipping row due to number format error in file {}: {}", csvPath, String.join(",", line), e);
                } catch (IllegalArgumentException e) {
                    log.error("Skipping row due to missing column in file {}: {}", csvPath, e.getMessage());
                }
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Failed to read CSV file: {}", csvPath, e);
            throw new RuntimeException("Failed to read CSV file: " + csvPath, e);
        }

        rows.sort(Comparator.comparingInt(PipelineRow::getOrder));
        return rows;
    }

    private static String getColumn(String[] line, String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                if (i < line.length) {
                    return line[i];
                }
                return null; // Column exists but no value in this row
            }
        }
        // Return null if the column is optional and not found
        if ("artifactJobName".equalsIgnoreCase(columnName) || "order".equalsIgnoreCase(columnName) || "variablesAndValues".equalsIgnoreCase(columnName)) {
            return null;
        }
        throw new IllegalArgumentException("Missing required column: " + columnName);
    }
}
