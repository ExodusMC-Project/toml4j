package com.moandjiezana.toml;

import java.util.concurrent.atomic.AtomicInteger;

record Context(Identifier identifier, AtomicInteger line, Results.Errors errors) {

    public Context with(Identifier identifier) {
        return new Context(identifier, this.line, this.errors);
    }
}
