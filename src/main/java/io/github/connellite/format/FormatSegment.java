package io.github.connellite.format;

sealed interface FormatSegment permits LiteralSegment, FieldSegment {
}
