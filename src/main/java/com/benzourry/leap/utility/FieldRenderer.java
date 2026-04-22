package com.benzourry.leap.utility;

import org.stringtemplate.v4.AttributeRenderer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;

public class FieldRenderer implements AttributeRenderer {

    public FieldRenderer() {
    }

    @Override
    public String toString(Object o, String formatString, Locale locale) {
        if (o == null) {
            return "";
        }

        if (formatString == null || formatString.isBlank()) {
            return o.toString();
        }

        // Split format string into type and argument, trimming whitespace
        String[] parts = formatString.split(":", 2);
        String type = parts[0].trim().toLowerCase();
        String arg = parts.length > 1 ? parts[1] : null;

        return switch (type) {
            case "date" -> formatDate(o, arg);
            case "number" -> formatNumber(o, arg, locale);
            case "string" -> formatString(o, arg, locale);
            case "src" -> formatSrc(o, arg);
            case "qr" -> IO_BASE_DOMAIN + "/api/form/qr?code=" + o;
            default -> o.toString();
        };
    }

    private String formatDate(Object o, String pattern) {
        if (pattern == null) return o.toString();

        Date d = null;
        if (o instanceof Date) {
            d = (Date) o;
        } else if (o instanceof Number) {
            d = new Date(((Number) o).longValue());
        } else {
            String strVal = o.toString();
            if (Helper.isValidLong(strVal)) {
                d = new Date(Long.parseLong(strVal));
            }
        }

        if (d != null) {
            DateFormat f = new SimpleDateFormat(pattern);
            return f.format(d);
        }
        return o.toString();
    }

    private String formatNumber(Object o, String format, Locale locale) {
        if (format == null) return o.toString();
        return String.format(locale, format, o);
    }

    private String formatString(Object o, String action, Locale locale) {
        String s = String.valueOf(o);
        if (action == null) return s;

        return switch (action) {
            case "upper" -> s.toUpperCase(locale);
            case "lower" -> s.toLowerCase(locale);
            case "cap" -> s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
            case "url-encode" -> URLEncoder.encode(s, StandardCharsets.UTF_8);
            case "xml-encode" -> escapeHTML(s);
            default -> String.format(action, s);
        };
    }

    private String formatSrc(Object o, String pathInfo) {
        if (pathInfo != null && !pathInfo.isBlank()) {
            return IO_BASE_DOMAIN + "/api/entry/file/" + pathInfo + "/" + o;
        } else {
            return IO_BASE_DOMAIN + "/api/entry/file/" + o;
        }
    }

    public static String escapeHTML(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder buf = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            switch (c) {
                case '\t', '\n', '\r' -> buf.append(c);
                case '&' -> buf.append("&amp;");
                case '<' -> buf.append("&lt;");
                case '>' -> buf.append("&gt;");
                default -> {
                    boolean control = c < ' ';
                    boolean aboveASCII = c > '~';
                    if (!control && !aboveASCII) {
                        buf.append(c);
                    } else {
                        buf.append("&#").append((int) c).append(";");
                    }
                }
            }
        }
        return buf.toString();
    }
}