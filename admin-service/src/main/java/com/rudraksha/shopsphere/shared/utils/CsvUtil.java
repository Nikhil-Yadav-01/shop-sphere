package com.rudraksha.shopsphere.shared.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CsvUtil {

    private static final char DEFAULT_DELIMITER = ',';
    private static final char QUOTE_CHAR = '"';

    private CsvUtil() {
    }

    public static List<String[]> parse(String csvContent) {
        return parse(csvContent, DEFAULT_DELIMITER);
    }

    public static List<String[]> parse(String csvContent, char delimiter) {
        if (csvContent == null || csvContent.isBlank()) {
            return Collections.emptyList();
        }
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    result.add(parseLine(line, delimiter));
                }
            }
        } catch (IOException e) {
            throw new CsvParseException("Failed to parse CSV content", e);
        }
        return result;
    }

    public static List<String[]> parse(InputStream inputStream) {
        return parse(inputStream, DEFAULT_DELIMITER);
    }

    public static List<String[]> parse(InputStream inputStream, char delimiter) {
        if (inputStream == null) {
            return Collections.emptyList();
        }
        List<String[]> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    result.add(parseLine(line, delimiter));
                }
            }
        } catch (IOException e) {
            throw new CsvParseException("Failed to parse CSV input stream", e);
        }
        return result;
    }

    public static List<Map<String, String>> parseWithHeaders(String csvContent) {
        return parseWithHeaders(csvContent, DEFAULT_DELIMITER);
    }

    public static List<Map<String, String>> parseWithHeaders(String csvContent, char delimiter) {
        List<String[]> rows = parse(csvContent, delimiter);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }
        String[] headers = rows.get(0);
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            String[] values = rows.get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                String value = j < values.length ? values[j] : "";
                row.put(headers[j].trim(), value.trim());
            }
            result.add(row);
        }
        return result;
    }

    public static String toCsv(List<String[]> rows) {
        return toCsv(rows, DEFAULT_DELIMITER);
    }

    public static String toCsv(List<String[]> rows, char delimiter) {
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            sb.append(formatRow(row, delimiter)).append("\n");
        }
        return sb.toString();
    }

    public static String toCsvWithHeaders(List<Map<String, String>> data, String[] headers) {
        return toCsvWithHeaders(data, headers, DEFAULT_DELIMITER);
    }

    public static String toCsvWithHeaders(List<Map<String, String>> data, String[] headers, char delimiter) {
        if (data == null || data.isEmpty() || headers == null || headers.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(formatRow(headers, delimiter)).append("\n");
        for (Map<String, String> row : data) {
            String[] values = new String[headers.length];
            for (int i = 0; i < headers.length; i++) {
                values[i] = row.getOrDefault(headers[i], "");
            }
            sb.append(formatRow(values, delimiter)).append("\n");
        }
        return sb.toString();
    }

    private static String[] parseLine(String line, char delimiter) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == QUOTE_CHAR) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == QUOTE_CHAR) {
                        current.append(QUOTE_CHAR);
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == QUOTE_CHAR) {
                    inQuotes = true;
                } else if (c == delimiter) {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private static String formatRow(String[] values, char delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            sb.append(escapeField(values[i], delimiter));
        }
        return sb.toString();
    }

    private static String escapeField(String field, char delimiter) {
        if (field == null) {
            return "";
        }
        boolean needsQuotes = field.contains(String.valueOf(delimiter))
                || field.contains("\"")
                || field.contains("\n")
                || field.contains("\r");
        if (needsQuotes) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    public static class CsvParseException extends RuntimeException {
        public CsvParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
