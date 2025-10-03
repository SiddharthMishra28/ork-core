package com.org.orchestrator.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class OutputEnvParser {
    public static Map<String, String> parse(InputStream is) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            br.lines()
              .filter(line -> !line.isBlank() && line.contains("="))
              .forEach(line -> {
                  int idx = line.indexOf('=');
                  map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
              });
        }
        return map;
    }
}
