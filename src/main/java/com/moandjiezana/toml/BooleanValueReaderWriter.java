package com.moandjiezana.toml;

import java.util.concurrent.atomic.AtomicInteger;


class BooleanValueReaderWriter implements ValueReader, ValueWriter {

    static final BooleanValueReaderWriter BOOLEAN_VALUE_READER_WRITER = new BooleanValueReaderWriter();

    @Override
    public boolean canRead(String s) {
        return s.startsWith("true") || s.startsWith("false");
    }

    @Override
    public Object read(String s, AtomicInteger index, Context context) {
        s = s.substring(index.get());
        final Boolean b = s.startsWith("true") ? Boolean.TRUE : Boolean.FALSE;

        final int endIndex = b == Boolean.TRUE ? 4 : 5;

        index.addAndGet(endIndex - 1);

        return b;
    }

    @Override
    public boolean canWrite(Object value) {
        return value instanceof Boolean;
    }

    @Override
    public void write(Object value, WriterContext context) {
        context.write(value.toString());
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    private BooleanValueReaderWriter() {
    }

    @Override
    public String toString() {
        return "boolean";
    }
}
