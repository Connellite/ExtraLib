/**
 * Internal {fmt}-style engine. Not exported by the module; do not use from client code.
 *
 * <p>Sources:
 * <ul>
 *   <li><a href="https://fmt.dev/11.2/syntax/">fmt 11.2 syntax</a></li>
 *   <li><a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h">format.h</a>
 *       ({@code write_escaped_string} at
 *       <a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/format.h#L1763">L1763</a>)</li>
 *   <li><a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/ranges.h">ranges.h</a></li>
 *   <li><a href="https://github.com/fmtlib/fmt/blob/11.2.0/include/fmt/chrono.h">chrono.h</a></li>
 * </ul>
 */
package io.github.connellite.format.internal;
