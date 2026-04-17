package game.save.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON parser/writer with no external dependencies.
 *
 * Supported types:
 * - Object: Map<String, Object>
 * - Array: List<Object>
 * - String, Double, Boolean, null
 *
 * Notes:
 * - Numbers are parsed as Double.
 * - Writer emits JSON with stable key order if using LinkedHashMap.
 */
public final class Json {

    private Json() {}

    public static Object parse(String json) {
        if (json == null) throw new IllegalArgumentException("json is null");
        Parser p = new Parser(json);
        Object v = p.parseValue();
        p.skipWhitespace();
        if (!p.isEof()) {
            throw new IllegalArgumentException("Trailing content at position " + p.pos);
        }
        return v;
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder(1024);
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String s) {
            sb.append('"').append(escapeString(s)).append('"');
            return;
        }
        if (value instanceof Boolean b) {
            sb.append(b ? "true" : "false");
            return;
        }
        if (value instanceof Integer i) {
            sb.append(i);
            return;
        }
        if (value instanceof Long l) {
            sb.append(l);
            return;
        }
        if (value instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) {
                sb.append("null");
            } else {
                // Keep it compact, but avoid scientific notation for simple integers.
                if (Math.rint(d) == d) sb.append((long) d.doubleValue());
                else sb.append(d);
            }
            return;
        }
        if (value instanceof Float f) {
            double d = f.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) sb.append("null");
            else {
                if (Math.rint(d) == d) sb.append((long) d);
                else sb.append(d);
            }
            return;
        }
        if (value instanceof Number n) {
            sb.append(n.toString());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (var e : map.entrySet()) {
                if (!(e.getKey() instanceof String)) {
                    throw new IllegalArgumentException("JSON object key must be String");
                }
                if (!first) sb.append(',');
                first = false;
                sb.append('"').append(escapeString((String) e.getKey())).append('"').append(':');
                writeValue(sb, e.getValue());
            }
            sb.append('}');
            return;
        }
        if (value instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
            return;
        }

        throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass());
    }

    private static String escapeString(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static final class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
        }

        boolean isEof() {
            return pos >= s.length();
        }

        void skipWhitespace() {
            while (!isEof()) {
                char c = s.charAt(pos);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') pos++;
                else break;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (isEof()) throw error("Unexpected EOF");
            char c = s.charAt(pos);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> {
                    if (c == '-' || (c >= '0' && c <= '9')) yield parseNumber();
                    throw error("Unexpected character: " + c);
                }
            };
        }

        private Object parseLiteral(String literal, Object value) {
            if (s.startsWith(literal, pos)) {
                pos += literal.length();
                return value;
            }
            throw error("Expected literal: " + literal);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> obj = new LinkedHashMap<>();
            if (peek('}')) {
                pos++;
                return obj;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                obj.put(key, value);
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                    continue;
                }
                if (peek('}')) {
                    pos++;
                    break;
                }
                throw error("Expected ',' or '}'");
            }
            return obj;
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> arr = new ArrayList<>();
            if (peek(']')) {
                pos++;
                return arr;
            }
            while (true) {
                Object v = parseValue();
                arr.add(v);
                skipWhitespace();
                if (peek(',')) {
                    pos++;
                    continue;
                }
                if (peek(']')) {
                    pos++;
                    break;
                }
                throw error("Expected ',' or ']'");
            }
            return arr;
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (!isEof()) {
                char c = s.charAt(pos++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (isEof()) throw error("Unterminated escape");
                    char e = s.charAt(pos++);
                    switch (e) {
                        case '"' -> out.append('"');
                        case '\\' -> out.append('\\');
                        case '/' -> out.append('/');
                        case 'b' -> out.append('\b');
                        case 'f' -> out.append('\f');
                        case 'n' -> out.append('\n');
                        case 'r' -> out.append('\r');
                        case 't' -> out.append('\t');
                        case 'u' -> {
                            if (pos + 4 > s.length()) throw error("Invalid unicode escape");
                            String hex = s.substring(pos, pos + 4);
                            pos += 4;
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException ex) {
                                throw error("Invalid unicode escape: " + hex);
                            }
                        }
                        default -> throw error("Invalid escape: \\" + e);
                    }
                } else {
                    out.append(c);
                }
            }
            throw error("Unterminated string");
        }

        private Double parseNumber() {
            int start = pos;
            if (peek('-')) pos++;
            while (!isEof() && Character.isDigit(s.charAt(pos))) pos++;
            if (!isEof() && s.charAt(pos) == '.') {
                pos++;
                while (!isEof() && Character.isDigit(s.charAt(pos))) pos++;
            }
            if (!isEof() && (s.charAt(pos) == 'e' || s.charAt(pos) == 'E')) {
                pos++;
                if (!isEof() && (s.charAt(pos) == '+' || s.charAt(pos) == '-')) pos++;
                while (!isEof() && Character.isDigit(s.charAt(pos))) pos++;
            }
            String num = s.substring(start, pos);
            try {
                return Double.parseDouble(num);
            } catch (NumberFormatException ex) {
                throw error("Invalid number: " + num);
            }
        }

        private boolean peek(char c) {
            return !isEof() && s.charAt(pos) == c;
        }

        private void expect(char c) {
            if (isEof() || s.charAt(pos) != c) {
                throw error("Expected '" + c + "'");
            }
            pos++;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at position " + pos);
        }
    }
}
