package io.github.connellite.format;

import java.util.Objects;

/** Named format argument; use {@link Fmt#arg(String, Object)}. */
public final class Named {

    public final String name;
    public final Object value;

    Named(String name, Object value) {
        this.name = Objects.requireNonNull(name, "name");
        this.value = value;
    }
}
