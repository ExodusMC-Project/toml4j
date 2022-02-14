package com.moandjiezana.toml;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class DateValueReaderWriter implements ValueReader, ValueWriter {

    static final DateValueReaderWriter DATE_VALUE_READER_WRITER = new DateValueReaderWriter();
    static final DateValueReaderWriter DATE_PARSER_JDK_6 = new DateConverterJdk6();
    private static final Pattern DATE_REGEX = Pattern.compile(
        "(\\d{4}-[0-1][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9])(\\.\\d*)?(Z|[+\\-]\\d{2}:\\d{2})(.*)");

    @Override
    public boolean canRead(String s) {
        if (s.length() < 5) {
            return false;
        }

        for (int i = 0; i < 5; i++) {
            final char c = s.charAt(i);

            if (i < 4) {
                if (!Character.isDigit(c)) {
                    return false;
                }
            } else if (c != '-') {
                return false;
            }
        }

        return true;
    }

    @Override
    public Object read(String original, AtomicInteger index, Context context) {
        final StringBuilder sb = new StringBuilder();

        for (int i = index.get(); i < original.length(); i = index.incrementAndGet()) {
            final char c = original.charAt(i);
            if (Character.isDigit(c) || c == '-' || c == '+' || c == ':' || c == '.' || c == 'T' || c == 'Z') {
                sb.append(c);
            } else {
                index.decrementAndGet();
                break;
            }
        }

        final String s = sb.toString();
        final Matcher matcher = DATE_REGEX.matcher(s);

        if (!matcher.matches()) {
            final Results.Errors errors = new Results.Errors();
            errors.invalidValue(context.identifier().getName(), s, context.line().get());
            return errors;
        }

        String dateString = matcher.group(1);
        final String zone = matcher.group(3);
        final String fractionalSeconds = matcher.group(2);
        String format = "yyyy-MM-dd'T'HH:mm:ss";
        if (fractionalSeconds != null && !fractionalSeconds.isEmpty()) {
            format += ".SSS";
            dateString += fractionalSeconds;
        }
        format += "Z";
        if ("Z".equals(zone)) {
            dateString += "+0000";
        } else if (zone.contains(":")) {
            dateString += zone.replace(":", "");
        }

        try {
            final SimpleDateFormat dateFormat = new SimpleDateFormat(format);
            dateFormat.setLenient(false);
            return dateFormat.parse(dateString);
        } catch (Exception e) {
            final Results.Errors errors = new Results.Errors();
            errors.invalidValue(context.identifier().getName(), s, context.line().get());
            return errors;
        }
    }

    @Override
    public boolean canWrite(Object value) {
        return value instanceof Date;
    }

    @Override
    public void write(Object value, WriterContext context) {
        final DateFormat formatter = this.getFormatter(context.getDatePolicy());
        context.write(formatter.format(value));
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    private DateFormat getFormatter(DatePolicy datePolicy) {
        final boolean utc = "UTC".equals(datePolicy.getTimeZone().getID());
        final String format;

        if (utc && datePolicy.isShowFractionalSeconds()) {
            format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        } else if (utc) {
            format = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        } else if (datePolicy.isShowFractionalSeconds()) {
            format = this.getTimeZoneAndFractionalSecondsFormat();
        } else {
            format = this.getTimeZoneFormat();
        }
        final SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(datePolicy.getTimeZone());

        return formatter;
    }

    String getTimeZoneFormat() {
        return "yyyy-MM-dd'T'HH:mm:ssXXX";
    }

    String getTimeZoneAndFractionalSecondsFormat() {
        return "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    }

    private DateValueReaderWriter() {
    }

    private static class DateConverterJdk6 extends DateValueReaderWriter {
        @Override
        public void write(Object value, WriterContext context) {
            final DateFormat formatter = super.getFormatter(context.getDatePolicy());
            final String date = formatter.format(value);

            if ("UTC".equals(context.getDatePolicy().getTimeZone().getID())) {
                context.write(date);
            } else {
                final int insertionIndex = date.length() - 2;
                context.write(date.substring(0, insertionIndex)).write(':').write(date.substring(insertionIndex));
            }
        }

        @Override
        String getTimeZoneAndFractionalSecondsFormat() {
            return "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        }

        @Override
        String getTimeZoneFormat() {
            return "yyyy-MM-dd'T'HH:mm:ssZ";
        }
    }

    @Override
    public String toString() {
        return "datetime";
    }
}
