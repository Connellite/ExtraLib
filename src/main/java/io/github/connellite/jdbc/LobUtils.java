package io.github.connellite.jdbc;

import lombok.experimental.UtilityClass;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

@UtilityClass
public class LobUtils {

    private static final int BUFFER_SIZE = 4096;

    /**
     * Reads all characters from {@code clob} via {@link Clob#getCharacterStream()} preserving
     * original line separators and all other characters.
     *
     * @param clob JDBC {@link Clob}; may be {@code null}
     * @return full text as stored in the CLOB, or {@code null} if {@code clob} is {@code null}
     */
    public static String convertClobToString(Clob clob) throws SQLException {
        if (clob == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try (Reader reader = clob.getCharacterStream()) {
            char[] buffer = new char[BUFFER_SIZE];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, read);
            }
        } catch (IOException e) {
            throw new SQLException(e);
        }
        return sb.toString();
    }

    /**
     * Reads all bytes from {@code blob} via {@link Blob#getBinaryStream()} until end of stream.
     *
     * @param blob JDBC {@link Blob}; may be {@code null}
     * @return a new byte array with the full contents, or {@code null} if {@code blob} is {@code null}
     */
    public static byte[] convertBlobToByteArray(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }
        try (InputStream in = blob.getBinaryStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Creates JDBC {@link Clob} from a {@link String}.
     *
     * @param s source text; may be {@code null}
     * @return a new {@link Clob}, or {@code null} if {@code s} is {@code null}
     */
    public static Clob createClob(String s) throws SQLException {
        if (s == null) {
            return null;
        }

        return new SerialClob(s.toCharArray());
    }

    /**
     * Creates JDBC {@link Clob} from a character array.
     *
     * @param chars source character array; may be {@code null}
     * @return a new {@link Clob}, or {@code null} if {@code chars} is {@code null}
     */
    public static Clob createClob(char[] chars) throws SQLException {
        if (chars == null) {
            return null;
        }
        return new SerialClob(chars);
    }

    /**
     * Creates JDBC {@link Blob} from a byte array.
     *
     * @param bytes source bytes; may be {@code null}
     * @return a new {@link Blob}, or {@code null} if {@code bytes} is {@code null}
     */
    public static Blob createBlob(byte[] bytes) throws SQLException {
        if (bytes == null) {
            return null;
        }
        return new SerialBlob(bytes);
    }
}
