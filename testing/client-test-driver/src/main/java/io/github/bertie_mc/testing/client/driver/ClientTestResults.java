package io.github.bertie_mc.testing.client.driver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class ClientTestResults {
    private ClientTestResults() {}

    static void write(Path target, List<TestResult> results) {
        long failures = results.stream().filter(TestResult::failed).count();
        double seconds = results.stream()
                .mapToDouble(result -> result.duration().toNanos() / 1_000_000_000.0)
                .sum();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<testsuite name=\"Bertie client tests\" tests=\"")
                .append(results.size())
                .append("\" failures=\"")
                .append(failures)
                .append("\" errors=\"0\" skipped=\"0\" time=\"")
                .append(seconds)
                .append("\">\n");
        for (TestResult result : results) {
            xml.append("  <testcase classname=\"")
                    .append(escape(className(result.name())))
                    .append("\" name=\"")
                    .append(escape(methodName(result.name())))
                    .append("\" time=\"")
                    .append(result.duration().toNanos() / 1_000_000_000.0)
                    .append("\"");
            if (!result.failed()) {
                xml.append("/>\n");
                continue;
            }
            StringWriter trace = new StringWriter();
            result.failure().printStackTrace(new PrintWriter(trace));
            xml.append(">\n    <failure message=\"")
                    .append(escape(result.failure().toString()))
                    .append("\" type=\"")
                    .append(escape(result.failure().getClass().getName()))
                    .append("\">")
                    .append(escape(trace.toString()))
                    .append("</failure>\n  </testcase>\n");
        }
        xml.append("</testsuite>\n");
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(target, xml, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write client-test results to " + target, exception);
        }
    }

    private static String className(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? "clienttest" : name.substring(0, separator);
    }

    private static String methodName(String name) {
        int separator = name.lastIndexOf('.');
        return separator < 0 ? name : name.substring(separator + 1);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
