package com.org.orchestrator.report;

import com.org.orchestrator.model.ExecutionSummary;
import com.org.orchestrator.model.PipelineResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class HtmlReportGenerator {

    private static final Logger log = LoggerFactory.getLogger(HtmlReportGenerator.class);

    public static void generate(List<ExecutionSummary> summaries, Path out) {
        log.info("Generating HTML report at: {}", out.toAbsolutePath());
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><meta charset='utf-8'><title>Execution Summary</title>");
            sb.append("<style>");
            sb.append("body{font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; margin: 40px; background-color: #f8f9fa; color: #212529;}");
            sb.append("h1{color: #343a40; border-bottom: 2px solid #dee2e6; padding-bottom: 10px;}");
            sb.append("h2{color: #495057;}");
            sb.append(".summary-box{border: 1px solid #dee2e6; border-radius: .25rem; margin-bottom: 2rem; background-color: #fff; box-shadow: 0 .125rem .25rem rgba(0,0,0,.075);}");
            sb.append(".summary-header{background-color: rgba(0,0,0,.03); padding: .75rem 1.25rem; border-bottom: 1px solid #dee2e6;}");
            sb.append(".status-PASSED{color: #28a745; font-weight: bold;}");
            sb.append(".status-FAILED{color: #dc3545; font-weight: bold;}");
            sb.append("table{width: 100%; border-collapse: collapse;}");
            sb.append("th, td{border: 1px solid #dee2e6; padding: .75rem; text-align: left;}");
            sb.append("thead{background-color: #e9ecef;}");
            sb.append("pre{background-color: #e9ecef; padding: 10px; border-radius: .25rem; white-space: pre-wrap; word-break: break-all;}");
            sb.append("</style>");
            sb.append("</head><body><h1>GitLab Pipeline Execution Summary</h1>");

            for (ExecutionSummary s : summaries) {
                sb.append("<div class='summary-box'>");
                sb.append("<div class='summary-header'><h2>").append(s.getCsvName()).append(" &mdash; <span class='status-").append(s.getStatus()).append("'>").append(s.getStatus()).append("</span></h2></div>");
                sb.append("<table><thead><tr><th>#</th><th>Pipeline ID</th><th>Status</th><th>Final Merged Variables</th></tr></thead><tbody>");
                int i = 1;
                for (PipelineResult pr : s.getPipelineResults()) {
                    sb.append("<tr>");
                    sb.append("<td>").append(i++).append("</td>");
                    sb.append("<td>").append(pr.getPipelineId() == -1 ? "N/A" : pr.getPipelineId()).append("</td>");
                    sb.append("<td>").append(pr.getStatus()).append("</td>");
                    sb.append("<td><pre>").append(mapToString(pr.getMergedVars())).append("</pre></td>");
                    sb.append("</tr>");
                }
                sb.append("</tbody></table></div>");
            }

            sb.append("</body></html>");
            Files.writeString(out, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Successfully generated HTML report.");
        } catch (IOException e) {
            log.error("Failed to generate HTML report.", e);
        }
    }

    private static String mapToString(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        map.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));
        sb.append("}");
        return sb.toString();
    }
}
