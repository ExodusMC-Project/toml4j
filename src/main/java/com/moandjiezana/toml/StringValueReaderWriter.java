package com.moandjiezana.toml;

import java.net.URI;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StringValueReaderWriter implements ValueReader, ValueWriter {

    static final StringValueReaderWriter STRING_VALUE_READER_WRITER = new StringValueReaderWriter();
    private static final Pattern UNICODE_REGEX = Pattern.compile("\\\\[uU](.{4})");
    private static final String NEWLINE_SEPERATOR = System.getProperty("line.separator");

    private static final String[] specialCharacterEscapes = new String[93];

    static {
        specialCharacterEscapes['\b'] = "\\b";
        specialCharacterEscapes['\t'] = "\\t";
        specialCharacterEscapes['\n'] = "\\n";
        specialCharacterEscapes['\f'] = "\\f";
        specialCharacterEscapes['\r'] = "\\r";
        specialCharacterEscapes['"'] = "\\\"";
        specialCharacterEscapes['\\'] = "\\\\";
    }

    @Override
    public boolean canRead(String s) {
        return s.startsWith("\"");
    }

    @Override
    public Object read(String s, AtomicInteger index, Context context) {
        final int startIndex = index.incrementAndGet();
        int endIndex = -1;

        for (int i = index.get(); i < s.length(); i = index.incrementAndGet()) {
            final char ch = s.charAt(i);
            if (ch == '"' && s.charAt(i - 1) != '\\') {
                endIndex = i;
                break;
            }
        }

        if (endIndex == -1) {
            final Results.Errors errors = new Results.Errors();
            errors.unterminated(context.identifier().getName(), s.substring(startIndex - 1), context.line().get());
            return errors;
        }

        final String raw = s.substring(startIndex, endIndex);
        s = this.replaceUnicodeCharacters(raw);
        s = this.replaceSpecialCharacters(s);

        if (s == null) {
            final Results.Errors errors = new Results.Errors();
            errors.invalidValue(context.identifier().getName(), raw, context.line().get());
            return errors;
        }

        return s;
    }

    String replaceUnicodeCharacters(String value) {
        final Matcher unicodeMatcher = UNICODE_REGEX.matcher(value);

        while (unicodeMatcher.find()) {
            value = value.replace(unicodeMatcher.group(), new String(Character.toChars(Integer.parseInt(unicodeMatcher.group(1), 16))));
        }
        return value;
    }

    String replaceSpecialCharacters(String s) {
        for (int i = 0; i < s.length() - 1; i++) {
            final char ch = s.charAt(i);
            final char next = s.charAt(i + 1);

            if (ch == '\\' && next == '\\') {
                i++;
            } else if (ch == '\\' && !(next == 'b' || next == 'f' || next == 'n' || next == 't' || next == 'r' || next == '"')) {
                return null;
            }
        }

        return s.replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\t", "\t")
            .replace("\\r", "\r")
            .replace("\\\\", "\\")
            .replace("\\/", "/")
            .replace("\\b", "\b")
            .replace("\\f", "\f");
    }

    @Override
    public boolean canWrite(Object value) {
        return value instanceof String || value instanceof Character || value instanceof URL || value instanceof URI || value instanceof Enum;
    }

    @Override
    public void write(Object value, WriterContext context) {
        final String literal = value.toString();

        if (literal.contains(NEWLINE_SEPERATOR)) {
            context.write("'''" + NEWLINE_SEPERATOR);
            this.escapeUnicodeMultiLine(value.toString(), context);
            context.write("'''");
        } else {
            context.write("\"");
            this.escapeUnicode(value.toString(), context);
            context.write("\"");
        }
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    private void escapeUnicode(String in, WriterContext context) {
        for (int i = 0; i < in.length(); i++) {
            final int codePoint = in.codePointAt(i);
            if (codePoint < specialCharacterEscapes.length && specialCharacterEscapes[codePoint] != null) {
                context.write(specialCharacterEscapes[codePoint]);
            } else {
                context.write(in.charAt(i));
            }
        }
    }

    private void escapeUnicodeMultiLine(String in, WriterContext context) {
        final String[] lines = in.split(NEWLINE_SEPERATOR);

        for (final String line : lines) {
            for (int i = 0; i < line.length(); i++) {
                final int codePoint = line.codePointAt(i);
                if (codePoint < specialCharacterEscapes.length && specialCharacterEscapes[codePoint] != null) {
                    context.write(specialCharacterEscapes[codePoint]);
                } else {
                    context.write(line.charAt(i));
                }
            }

            context.write(NEWLINE_SEPERATOR);
        }
    }

    private StringValueReaderWriter() {
    }

    @Override
    public String toString() {
        return "string";
    }
}
