package com.beatmaker.core.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal, dependency-free JSON reader/writer.
 *
 * Supported value types when writing: {@code Map<String, ?>} (object),
 * {@code List<?>} (array), {@code String}, {@code Number}, {@code Boolean} and {@code null}.
 *
 * Parsing returns the same generic shapes: {@code Map<String, Object>}, {@code List<Object>},
 * {@code String}, {@code Long}/{@code Double}, {@code Boolean} or {@code null}.
 */
public class Json {

    private Json() {}

    // ---------------------------------------------------------------- write

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object value) {
        if (null == value) {
            sb.append("null");
        } else if (value instanceof Map) {
            writeObject(sb, (Map<String, Object>) value);
        } else if (value instanceof List) {
            writeArray(sb, (List<Object>) value);
        } else if (value instanceof String) {
            writeString(sb, (String) value);
        } else if (value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Number) {
            sb.append(value.toString());
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeObject(StringBuilder sb, Map<String, Object> map) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, entry.getKey());
            sb.append(':');
            writeValue(sb, entry.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<Object> list) {
        sb.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(',');
            first = false;
            writeValue(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    // ----------------------------------------------------------------- read

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        return value;
    }

    private static class Parser {
        private final String text;
        private int pos;

        Parser(String text) {
            this.text = text;
            this.pos = 0;
        }

        void skipWhitespace() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            return text.charAt(pos);
        }

        private char next() {
            return text.charAt(pos++);
        }

        private void expect(char c) {
            skipWhitespace();
            if (pos >= text.length() || text.charAt(pos) != c) {
                throw new JsonParseException("Expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        Object parseValue() {
            skipWhitespace();
            if (pos >= text.length()) {
                throw new JsonParseException("Unexpected end of input");
            }

            char c = peek();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (pos < text.length() && peek() == '}') {
                pos++;
                return map;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (pos < text.length() && peek() == ',') {
                    pos++;
                    continue;
                }
                break;
            }

            expect('}');
            return map;
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (pos < text.length() && peek() == ']') {
                pos++;
                return list;
            }

            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (pos < text.length() && peek() == ',') {
                    pos++;
                    continue;
                }
                break;
            }

            expect(']');
            return list;
        }

        private String parseString() {
            skipWhitespace();
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (pos >= text.length()) {
                    throw new JsonParseException("Unterminated string");
                }
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            String hex = text.substring(pos, pos + 4);
                            pos += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default:
                            throw new JsonParseException("Invalid escape sequence: \\" + esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Boolean parseBoolean() {
            if (text.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            } else if (text.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new JsonParseException("Invalid literal at position " + pos);
        }

        private Object parseNull() {
            if (text.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new JsonParseException("Invalid literal at position " + pos);
        }

        private Number parseNumber() {
            int start = pos;
            if (pos < text.length() && (peek() == '-' || peek() == '+')) pos++;
            boolean isFloat = false;
            while (pos < text.length()) {
                char c = peek();
                if (Character.isDigit(c)) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '-' || c == '+') {
                    isFloat = true;
                    pos++;
                } else {
                    break;
                }
            }
            String numText = text.substring(start, pos);
            if (numText.isEmpty()) {
                throw new JsonParseException("Invalid number at position " + pos);
            }
            if (isFloat) {
                return Double.parseDouble(numText);
            }
            return Long.parseLong(numText);
        }
    }

    public static class JsonParseException extends RuntimeException {
        public JsonParseException(String message) {
            super(message);
        }
    }

    // --------------------------------------------------------------- helpers

    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    public static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new LinkedHashMap<>();
    }
}
