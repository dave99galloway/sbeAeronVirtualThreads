package com.playground.sbeaeronvirtualthreads.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates an HTML report from JMH benchmark JSON results.
 * 
 * Usage: java JmhReportGenerator <json-file> [output-file]
 */
public class JmhReportGenerator {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat SCI_FORMAT = new DecimalFormat("0.###E0");
    
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java JmhReportGenerator <json-file> [output-file]");
            System.exit(1);
        }
        
        String jsonFile = args[0];
        String htmlFile = args.length > 1 ? args[1] : jsonFile.replace(".json", ".html");
        
        generateReport(jsonFile, htmlFile);
        System.out.println("HTML report generated: " + htmlFile);
    }
    
    public static void generateReport(String jsonFile, String htmlFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode results = mapper.readTree(new File(jsonFile));
        
        List<BenchmarkResult> benchmarks = new ArrayList<>();
        for (JsonNode result : results) {
            benchmarks.add(parseBenchmark(result));
        }
        
        String html = generateHtml(benchmarks);
        try (FileWriter writer = new FileWriter(htmlFile)) {
            writer.write(html);
        }
    }
    
    private static BenchmarkResult parseBenchmark(JsonNode result) {
        String benchmark = result.get("benchmark").asText();
        String methodName = benchmark.substring(benchmark.lastIndexOf('.') + 1);
        
        JsonNode metric = result.get("primaryMetric");
        double score = metric.get("score").asDouble();
        double error = metric.get("scoreError").asDouble();
        String unit = metric.get("scoreUnit").asText();
        
        JsonNode percentiles = metric.get("scorePercentiles");
        double p50 = percentiles.get("50.0").asDouble();
        double p95 = percentiles.get("95.0").asDouble();
        double p99 = percentiles.get("99.0").asDouble();
        
        return new BenchmarkResult(methodName, score, error, unit, p50, p95, p99);
    }
    
    private static String generateHtml(List<BenchmarkResult> benchmarks) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>JMH Benchmark Results</title>\n");
        html.append("    <style>\n");
        html.append(getStyles());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class=\"container\">\n");
        html.append("        <h1>🚀 JMH Benchmark Results</h1>\n");
        html.append("        <p class=\"subtitle\">Aeron Serialization Performance Comparison</p>\n");
        
        // Summary section
        html.append("        <div class=\"summary\">\n");
        html.append("            <h2>Summary</h2>\n");
        html.append("            <p><strong>Total Benchmarks:</strong> ").append(benchmarks.size()).append("</p>\n");
        html.append("            <p><strong>Mode:</strong> Throughput (operations per nanosecond)</p>\n");
        html.append("            <p class=\"note\">⚠️ <strong>Important:</strong> All scores are in <code>ops/ns</code> (operations per nanosecond). ");
        html.append("These are extremely small numbers because each benchmark iteration involves full Aeron messaging setup/teardown. ");
        html.append("For meaningful comparisons, focus on the <strong>relative performance</strong> between formats.</p>\n");
        html.append("        </div>\n");
        
        // Results table
        html.append("        <table>\n");
        html.append("            <thead>\n");
        html.append("                <tr>\n");
        html.append("                    <th>Benchmark</th>\n");
        html.append("                    <th title=\"Average throughput (operations per nanosecond)\">Score (ops/ns)</th>\n");
        html.append("                    <th title=\"99.9% confidence interval - the true score is likely within Score ± Error\">Error (±)</th>\n");
        html.append("                    <th title=\"50th percentile (median) - half of measurements were below this\">P50</th>\n");
        html.append("                    <th title=\"95th percentile - 95% of measurements were below this\">P95</th>\n");
        html.append("                    <th title=\"99th percentile - 99% of measurements were below this\">P99</th>\n");
        html.append("                    <th title=\"Performance relative to SBE baseline\">Relative</th>\n");
        html.append("                </tr>\n");
        html.append("            </thead>\n");
        html.append("            <tbody>\n");
        
        // Find baseline (SBE without virtual threads)
        double baselineScore = benchmarks.stream()
            .filter(b -> b.name.equals("benchmarkSbeSerialization"))
            .findFirst()
            .map(b -> b.score)
            .orElse(1.0);
        
        for (BenchmarkResult benchmark : benchmarks) {
            html.append("                <tr class=\"").append(getRowClass(benchmark.name)).append("\">\n");
            html.append("                    <td class=\"benchmark-name\">").append(formatBenchmarkName(benchmark.name)).append("</td>\n");
            html.append("                    <td>").append(SCI_FORMAT.format(benchmark.score)).append("</td>\n");
            html.append("                    <td>").append(SCI_FORMAT.format(benchmark.error)).append("</td>\n");
            html.append("                    <td>").append(SCI_FORMAT.format(benchmark.p50)).append("</td>\n");
            html.append("                    <td>").append(SCI_FORMAT.format(benchmark.p95)).append("</td>\n");
            html.append("                    <td>").append(SCI_FORMAT.format(benchmark.p99)).append("</td>\n");
            html.append("                    <td>").append(formatRelative(benchmark.score / baselineScore)).append("</td>\n");
            html.append("                </tr>\n");
        }
        
        html.append("            </tbody>\n");
        html.append("        </table>\n");
        
        // Comparison charts
        html.append(generateComparisonSection(benchmarks, baselineScore));
        
        // Interpretation guide
        html.append(generateInterpretationGuide());
        
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }
    
    private static String getStyles() {
        return """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                line-height: 1.6;
                color: #333;
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                padding: 20px;
            }
            
            .container {
                max-width: 1200px;
                margin: 0 auto;
                background: white;
                border-radius: 10px;
                box-shadow: 0 10px 30px rgba(0,0,0,0.2);
                padding: 40px;
            }
            
            h1 {
                color: #667eea;
                margin-bottom: 10px;
                font-size: 2.5em;
            }
            
            .subtitle {
                color: #666;
                font-size: 1.2em;
                margin-bottom: 30px;
            }
            
            h2 {
                color: #764ba2;
                margin-top: 30px;
                margin-bottom: 15px;
                font-size: 1.8em;
                border-bottom: 2px solid #667eea;
                padding-bottom: 10px;
            }
            
            .summary {
                background: #f8f9fa;
                border-left: 4px solid #667eea;
                padding: 20px;
                margin: 20px 0;
                border-radius: 5px;
            }
            
            .summary p {
                margin: 10px 0;
            }
            
            .note {
                background: #fff3cd;
                border: 1px solid #ffc107;
                padding: 15px;
                border-radius: 5px;
                margin-top: 15px;
            }
            
            table {
                width: 100%;
                border-collapse: collapse;
                margin: 20px 0;
                box-shadow: 0 2px 5px rgba(0,0,0,0.1);
            }
            
            thead {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
            }
            
            th {
                padding: 15px;
                text-align: left;
                font-weight: 600;
            }
            
            td {
                padding: 12px 15px;
                border-bottom: 1px solid #ddd;
            }
            
            tr:hover {
                background: #f5f5f5;
            }
            
            .sbe { background: rgba(40, 167, 69, 0.1); }
            .protobuf { background: rgba(0, 123, 255, 0.1); }
            .json { background: rgba(255, 193, 7, 0.1); }
            
            .benchmark-name {
                font-weight: 600;
                color: #333;
            }
            
            code {
                background: #f4f4f4;
                padding: 2px 6px;
                border-radius: 3px;
                font-family: 'Courier New', monospace;
                font-size: 0.9em;
            }
            
            .comparison-section {
                margin: 30px 0;
                padding: 20px;
                background: #f8f9fa;
                border-radius: 5px;
            }
            
            .comparison-row {
                display: flex;
                align-items: center;
                margin: 15px 0;
                padding: 10px;
                background: white;
                border-radius: 5px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.05);
            }
            
            .format-label {
                min-width: 200px;
                font-weight: 600;
                color: #333;
            }
            
            .bar-container {
                flex: 1;
                height: 30px;
                background: #e9ecef;
                border-radius: 15px;
                overflow: hidden;
                position: relative;
            }
            
            .bar {
                height: 100%;
                display: flex;
                align-items: center;
                padding-left: 10px;
                color: white;
                font-weight: 600;
                font-size: 0.9em;
                transition: width 0.3s ease;
            }
            
            .bar-sbe { background: linear-gradient(90deg, #28a745, #20c997); }
            .bar-protobuf { background: linear-gradient(90deg, #007bff, #6610f2); }
            .bar-json { background: linear-gradient(90deg, #ffc107, #fd7e14); }
            
            .interpretation {
                background: #e7f3ff;
                border-left: 4px solid #007bff;
                padding: 20px;
                margin: 30px 0;
                border-radius: 5px;
            }
            
            .interpretation ul {
                margin-left: 20px;
                margin-top: 10px;
            }
            
            .interpretation li {
                margin: 8px 0;
            }
            
            strong {
                color: #667eea;
            }
        """;
    }
    
    private static String formatBenchmarkName(String name) {
        return name
            .replace("benchmark", "")
            .replace("Serialization", "")
            .replace("WithVirtualThreads", " (Virtual Threads)")
            .replaceAll("([A-Z])", " $1")
            .trim();
    }
    
    private static String getRowClass(String name) {
        if (name.toLowerCase().contains("sbe")) return "sbe";
        if (name.toLowerCase().contains("protobuf")) return "protobuf";
        if (name.toLowerCase().contains("json")) return "json";
        return "";
    }
    
    private static String formatRelative(double relative) {
        if (relative >= 1.0) {
            return String.format("%.2fx <span style=\"color: green\">↑</span>", relative);
        } else {
            return String.format("%.2fx <span style=\"color: red\">↓</span>", relative);
        }
    }
    
    private static String generateComparisonSection(List<BenchmarkResult> benchmarks, double baselineScore) {
        StringBuilder html = new StringBuilder();
        html.append("        <div class=\"comparison-section\">\n");
        html.append("            <h2>Relative Performance Comparison</h2>\n");
        html.append("            <p style=\"margin-bottom: 20px;\">Normalized to SBE without Virtual Threads (baseline = 1.0x)</p>\n");
        
        // Platform threads comparison
        html.append("            <h3 style=\"margin-top: 20px;\">Platform Threads</h3>\n");
        addComparisonBar(html, "SBE", findScore(benchmarks, "benchmarkSbeSerialization"), baselineScore, "sbe");
        addComparisonBar(html, "Protobuf", findScore(benchmarks, "benchmarkProtobufSerialization"), baselineScore, "protobuf");
        addComparisonBar(html, "JSON", findScore(benchmarks, "benchmarkJsonSerialization"), baselineScore, "json");
        
        // Virtual threads comparison
        html.append("            <h3 style=\"margin-top: 30px;\">Virtual Threads</h3>\n");
        addComparisonBar(html, "SBE (Virtual)", findScore(benchmarks, "benchmarkSbeWithVirtualThreads"), baselineScore, "sbe");
        addComparisonBar(html, "Protobuf (Virtual)", findScore(benchmarks, "benchmarkProtobufWithVirtualThreads"), baselineScore, "protobuf");
        addComparisonBar(html, "JSON (Virtual)", findScore(benchmarks, "benchmarkJsonWithVirtualThreads"), baselineScore, "json");
        
        html.append("        </div>\n");
        return html.toString();
    }
    
    private static double findScore(List<BenchmarkResult> benchmarks, String name) {
        return benchmarks.stream()
            .filter(b -> b.name.equals(name))
            .findFirst()
            .map(b -> b.score)
            .orElse(0.0);
    }
    
    private static void addComparisonBar(StringBuilder html, String label, double score, double baseline, String type) {
        double relative = score / baseline;
        double percentage = relative * 100;
        
        html.append("            <div class=\"comparison-row\">\n");
        html.append("                <div class=\"format-label\">").append(label).append("</div>\n");
        html.append("                <div class=\"bar-container\">\n");
        html.append("                    <div class=\"bar bar-").append(type).append("\" style=\"width: ").append(percentage).append("%\">\n");
        html.append("                        ").append(String.format("%.2fx", relative)).append("\n");
        html.append("                    </div>\n");
        html.append("                </div>\n");
        html.append("            </div>\n");
    }
    
    private static String generateInterpretationGuide() {
        return """
                <div class="interpretation">
                    <h2>📊 How to Interpret These Results</h2>
                    
                    <h3>Understanding the Columns</h3>
                    <ul>
                        <li><strong>Score (ops/ns):</strong> Average throughput in operations per nanosecond. Very small numbers because each benchmark includes full Aeron setup/teardown overhead</li>
                        <li><strong>Error (±):</strong> Statistical margin of error for the score at 99.9% confidence level. The true score is likely within Score ± Error. Smaller error indicates more consistent/reliable measurements</li>
                        <li><strong>P50 (Median):</strong> 50th percentile - half of measurements were below this value</li>
                        <li><strong>P95:</strong> 95th percentile - 95% of measurements were below this (filters out most outliers)</li>
                        <li><strong>P99:</strong> 99th percentile - 99% of measurements were below this (filters out nearly all outliers)</li>
                        <li><strong>Relative:</strong> Performance compared to SBE baseline (1.0x). Higher is better</li>
                    </ul>
                    
                    <h3>Understanding the Scores</h3>
                    <ul>
                        <li><strong>ops/ns (operations per nanosecond):</strong> Very small numbers because each benchmark includes full Aeron setup/teardown overhead</li>
                        <li><strong>P50/P95/P99:</strong> Percentile measurements showing consistency (lower variance is better)</li>
                        <li><strong>Relative Performance:</strong> Compares against SBE baseline (1.0x)</li>
                    </ul>
                    
                    <h3>What These Benchmarks Measure</h3>
                    <ul>
                        <li>Complete round-trip: serialize → publish → subscribe → deserialize</li>
                        <li>Includes Aeron Media Driver overhead</li>
                        <li>Includes thread startup/teardown for Virtual Thread tests</li>
                    </ul>
                    
                    <h3>Key Takeaways</h3>
                    <ul>
                        <li><strong>SBE:</strong> Fastest serialization, lowest overhead, best for ultra-low latency</li>
                        <li><strong>Protobuf:</strong> Good balance of speed and flexibility</li>
                        <li><strong>JSON:</strong> Slower but human-readable, best for debugging/APIs</li>
                        <li><strong>Virtual Threads:</strong> May show similar or slightly different performance due to scheduling overhead vs. resource efficiency</li>
                    </ul>
                    
                    <h3>For Real-World Performance</h3>
                    <p>Run the <code>PerformanceBenchmarkTest</code> which measures sustained throughput over many messages:</p>
                    <pre><code>./gradlew test --tests "*PerformanceBenchmarkTest"</code></pre>
                    <p>These JMH benchmarks are useful for micro-optimization and understanding relative differences between formats.</p>
                </div>
        """;
    }
    
    private static class BenchmarkResult {
        final String name;
        final double score;
        final double error;
        final String unit;
        final double p50;
        final double p95;
        final double p99;
        
        BenchmarkResult(String name, double score, double error, String unit, double p50, double p95, double p99) {
            this.name = name;
            this.score = score;
            this.error = error;
            this.unit = unit;
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
        }
    }
}
