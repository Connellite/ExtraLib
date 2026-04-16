module io.github.connellite.ExtraLib {
    requires java.logging;
    requires java.management;
    requires java.sql;
    requires java.sql.rowset;
    requires jdk.management;
    requires static lombok;
    requires java.desktop;

    exports io.github.connellite.collections;
    exports io.github.connellite.concurrent;
    exports io.github.connellite.exception;
    exports io.github.connellite.format;
    exports io.github.connellite.jdbc;
    exports io.github.connellite.logger;
    exports io.github.connellite.match;
    exports io.github.connellite.system;
    exports io.github.connellite.cloner;
    exports io.github.connellite.util;
}