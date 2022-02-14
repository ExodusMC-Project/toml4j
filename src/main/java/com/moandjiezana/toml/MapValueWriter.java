package com.moandjiezana.toml;

import static com.moandjiezana.toml.PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER;
import static com.moandjiezana.toml.TableArrayValueWriter.TABLE_ARRAY_VALUE_WRITER;
import static com.moandjiezana.toml.ValueWriters.WRITERS;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MapValueWriter implements ValueWriter {
    static final ValueWriter MAP_VALUE_WRITER = new MapValueWriter();

    private static final Pattern REQUIRED_QUOTING_PATTERN = Pattern.compile("^.*[^A-Za-z\\d_-].*$");

    @Override
    public boolean canWrite(Object value) {
        return value instanceof Map;
    }

    @Override
    public void write(Object value, WriterContext context) {
        final Map<?, ?> from = (Map<?, ?>) value;

        if (hasPrimitiveValues(from, context)) {
            context.writeKey();
        }

        // Render primitive types and arrays of primitive first so they are
        // grouped under the same table (if there is one)
        for (Map.Entry<?, ?> entry : from.entrySet()) {
            final Object key = entry.getKey();
            final Object fromValue = entry.getValue();
            if (fromValue == null) {
                continue;
            }

            final ValueWriter valueWriter = WRITERS.findWriterFor(fromValue);
            if (valueWriter.isPrimitiveType()) {
                context.indent();
                context.write(quoteKey(key)).write(" = ");
                valueWriter.write(fromValue, context);
                context.write('\n');
            } else if (valueWriter == PRIMITIVE_ARRAY_VALUE_WRITER) {
                context.setArrayKey(key.toString());
                context.write(quoteKey(key)).write(" = ");
                valueWriter.write(fromValue, context);
                context.write('\n');
            }
        }

        // Now render (sub)tables and arrays of tables
        for (Object key : from.keySet()) {
            final Object fromValue = from.get(key);
            if (fromValue == null) {
                continue;
            }

            final ValueWriter valueWriter = WRITERS.findWriterFor(fromValue);
            if (valueWriter == this || valueWriter == ObjectValueWriter.OBJECT_VALUE_WRITER || valueWriter == TABLE_ARRAY_VALUE_WRITER) {
                valueWriter.write(fromValue, context.pushTable(quoteKey(key)));
            }
        }
    }

    @Override
    public boolean isPrimitiveType() {
        return false;
    }

    private static String quoteKey(Object key) {
        String stringKey = key.toString();
        final Matcher matcher = REQUIRED_QUOTING_PATTERN.matcher(stringKey);
        if (matcher.matches()) {
            stringKey = "\"" + stringKey + "\"";
        }

        return stringKey;
    }

    @SuppressWarnings("unused")
    private static boolean hasPrimitiveValues(Map<?, ?> values, WriterContext context) {
        for (Object key : values.keySet()) {
            final Object fromValue = values.get(key);
            if (fromValue == null) {
                continue;
            }

            final ValueWriter valueWriter = WRITERS.findWriterFor(fromValue);
            if (valueWriter.isPrimitiveType() || valueWriter == PRIMITIVE_ARRAY_VALUE_WRITER) {
                return true;
            }
        }

        return false;
    }

    private MapValueWriter() {
    }
}
