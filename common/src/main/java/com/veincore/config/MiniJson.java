package com.veincore.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JSON 读写（零第三方依赖，够本项目配置文件用）。
 * 支持：对象、数组、字符串、数字、布尔、null；数字一律读成 Double。
 */
public final class MiniJson {

    private MiniJson() {
    }

    public static Object parse(String text) {
        Parser p = new Parser(text);
        Object value = p.parseValue();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw new IllegalArgumentException("JSON 尾部有多余字符，位置 " + p.index());
        }
        return value;
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String s) {
            writeString(s, sb);
            return;
        }
        if (value instanceof Boolean b) {
            sb.append(b);
            return;
        }
        if (value instanceof Number n) {
            if (n instanceof Double d && d == Math.rint(d) && !d.isInfinite()) {
                sb.append((long) (double) d);
            } else {
                sb.append(n);
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
            return;
        }
        if (value instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object o : list) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                write(o, sb);
            }
            sb.append(']');
            return;
        }
        throw new IllegalArgumentException("不支持的 JSON 值类型：" + value.getClass());
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
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
        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
        }

        int index() {
            return i;
        }

        boolean atEnd() {
            return i >= s.length();
        }

        void skipWhitespace() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        Object parseValue() {
            skipWhitespace();
            if (atEnd()) {
                throw err("JSON 意外结束");
            }
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            i++;
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek() == '}') {
                i++;
                return map;
            }
            while (true) {
                skipWhitespace();
                if (peek() != '"') {
                    throw err("对象键必须是字符串");
                }
                String key = parseString();
                skipWhitespace();
                if (peek() != ':') {
                    throw err("对象键后应为冒号");
                }
                i++;
                map.put(key, parseValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    i++;
                    continue;
                }
                if (c == '}') {
                    i++;
                    return map;
                }
                throw err("对象中应为逗号或右花括号");
            }
        }

        private List<Object> parseArray() {
            i++;
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek() == ']') {
                i++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                char c = peek();
                if (c == ',') {
                    i++;
                    continue;
                }
                if (c == ']') {
                    i++;
                    return list;
                }
                throw err("数组中应为逗号或右方括号");
            }
        }

        private String parseString() {
            i++;
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw err("字符串未结束");
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        String hex = s.substring(i, i + 4);
                        i += 4;
                        sb.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw err("非法转义：" + e);
                }
            }
        }

        private Object parseNumber() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            if (start == i) {
                throw err("非法 JSON 字符：" + s.charAt(start));
            }
            return Double.parseDouble(s.substring(start, i));
        }

        private Object literal(String text, Object value) {
            if (!s.startsWith(text, i)) {
                throw err("非法字面量");
            }
            i += text.length();
            return value;
        }

        private char peek() {
            if (atEnd()) {
                throw err("JSON 意外结束");
            }
            return s.charAt(i);
        }

        private IllegalArgumentException err(String msg) {
            return new IllegalArgumentException(msg + "，位置 " + i);
        }
    }
}
