package io.github.connellite.format;

import java.util.Objects;

/** Named format argument; use {@link Fmt#arg(String, Object)}. */
public record Named(String name, Object value) {

    public Named {
        Objects.requireNonNull(name, "name");
    }
}
