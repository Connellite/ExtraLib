package io.github.connellite.format;

/** Automatic field {@code {}} — next positional (non-{@link Named}) argument. */
final class AutoArgId extends ArgId {

    final int slot;

    AutoArgId(int slot) {
        this.slot = slot;
    }
}
