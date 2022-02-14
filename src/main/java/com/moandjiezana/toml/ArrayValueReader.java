package com.moandjiezana.toml;

import static com.moandjiezana.toml.ValueReaders.VALUE_READERS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class ArrayValueReader implements ValueReader {

    static final ArrayValueReader ARRAY_VALUE_READER = new ArrayValueReader();

    @Override
    public boolean canRead(String s) {
        return s.startsWith("[");
    }

    @Override
    public Object read(String s, AtomicInteger index, Context context) {
        final AtomicInteger line = context.line();
        final int startLine = line.get();
        final int startIndex = index.get();
        final List<Object> arrayItems = new ArrayList<>();
        boolean terminated = false;
        boolean inComment = false;
        final Results.Errors errors = new Results.Errors();

        for (int i = index.incrementAndGet(); i < s.length(); i = index.incrementAndGet()) {

            final char c = s.charAt(i);

            if (c == '#' && !inComment) {
                inComment = true;
            } else if (c == '\n') {
                inComment = false;
                line.incrementAndGet();
            } else if (inComment || Character.isWhitespace(c) || c == ',') {
                continue;
            } else if (c == '[') {
                final Object converted = this.read(s, index, context);
                if (converted instanceof Results.Errors) {
                    errors.add((Results.Errors) converted);
                } else if (!this.isHomogenousArray(converted, arrayItems)) {
                    errors.heterogenous(context.identifier().getName(), line.get());
                } else {
                    arrayItems.add(converted);
                }
                continue;
            } else if (c == ']') {
                terminated = true;
                break;
            } else {
                final Object converted = VALUE_READERS.convert(s, index, context);
                if (converted instanceof Results.Errors) {
                    errors.add((Results.Errors) converted);
                } else if (!this.isHomogenousArray(converted, arrayItems)) {
                    errors.heterogenous(context.identifier().getName(), line.get());
                } else {
                    arrayItems.add(converted);
                }
            }
        }

        if (!terminated) {
            errors.unterminated(context.identifier().getName(), s.substring(startIndex), startLine);
        }

        if (errors.hasErrors()) {
            return errors;
        }

        return arrayItems;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isHomogenousArray(Object o, List<?> values) {
        return values.isEmpty() || values.get(0).getClass().isAssignableFrom(o.getClass()) || o.getClass().isAssignableFrom(values.get(0).getClass());
    }

    private ArrayValueReader() {
    }
}
