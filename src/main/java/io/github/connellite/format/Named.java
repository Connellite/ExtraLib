package io.github.connellite.format;

import lombok.NonNull;

/** Named format argument; use {@link Fmt#arg(String, Object)}. */
public record Named (@NonNull String name, Object value) {

}
