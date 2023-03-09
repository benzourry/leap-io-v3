package com.benzourry.leap.utility;

import org.stringtemplate.v4.AttributeRenderer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;

public class FieldRenderer implements AttributeRenderer {
    public FieldRenderer() {
    }

    public String toString(Object o, String formatString, Locale locale) {
//        System.out.println("#########"+formatString);
        String output = "";

        if (formatString!=null){
            if (formatString.contains("date:")){
                String [] splitted = formatString.split(":",2);

                String val;
                if (Helper.isValidLong(o.toString())){
                    Date d = new Date(Long.parseLong(o.toString()));
                    DateFormat f = new SimpleDateFormat(splitted[1]);
                    val = f.format(d);
                }else{
                    val = o.toString();
                }
                return val;
            }else if (formatString.contains("number:")){
                String [] splitted =  formatString.split(":",2);
                Formatter f = new Formatter(locale);
                f.format(splitted[1], o);
                return f.toString();
            }else if (formatString.contains("string:")){
                String [] splitted = formatString.split(":",2);
                String s = (String)o;
                if (splitted[1] == null) {
                    return s;
                } else if (splitted[1].equals("upper")) {
                    return s.toUpperCase(locale);
                } else if (splitted[1].equals("lower")) {
                    return s.toLowerCase(locale);
                } else if (splitted[1].equals("cap")) {
                    return s.length() > 0 ? Character.toUpperCase(s.charAt(0)) + s.substring(1) : s;
                } else if (splitted[1].equals("url-encode")) {
                    return URLEncoder.encode(s, StandardCharsets.UTF_8);
                } else {
                    return splitted[1].equals("xml-encode") ? escapeHTML(s) : String.format(splitted[1], s);
                }
            }else if (formatString.contains("src")){
                return IO_BASE_DOMAIN + "/api/entry/file/"+o;
            }else if (formatString.contains("qr")){
                return IO_BASE_DOMAIN + "/api/form/qr?code="+o;
//                String [] splitted = formatString.split(":",2);
//                String s = (String)o;
//                if (splitted[1] == null) {
//                    return s;
//                } else if (splitted[1].equals("upper")) {
//                    return s.toUpperCase(locale);
//                } else if (splitted[1].equals("lower")) {
//                    return s.toLowerCase(locale);
//                } else if (splitted[1].equals("cap")) {
//                    return s.length() > 0 ? Character.toUpperCase(s.charAt(0)) + s.substring(1) : s;
//                } else if (splitted[1].equals("url-encode")) {
//                    return URLEncoder.encode(s);
//                } else {
//                    return splitted[1].equals("xml-encode") ? escapeHTML(s) : String.format(splitted[1], s);
//                }
            }
        }else{
            output = o.toString();
        }

        return output;
    }

    public static String escapeHTML(String s) {
        if (s == null) {
            return null;
        } else {
            StringBuilder buf = new StringBuilder(s.length());
            int len = s.length();

            for(int i = 0; i < len; ++i) {
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
                            buf.append("&#");
                            buf.append(c);
                            buf.append(";");
                        }
                    }
                }
            }

            return buf.toString();
        }
    }
}
