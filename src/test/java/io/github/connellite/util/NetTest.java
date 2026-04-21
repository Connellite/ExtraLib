package io.github.connellite.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetTest {

    @Test
    void address2Host_localhostIsLoopback() throws Exception {
        String addr = Net.address2Host("localhost");
        assertTrue("127.0.0.1".equals(addr) || "0:0:0:0:0:0:0:1".equals(addr) || "::1".equals(addr),
                () -> "unexpected localhost address: " + addr);
    }

    @Test
    void address2Host_numericIpv4_roundTrip() throws Exception {
        assertEquals("192.0.2.1", Net.address2Host("192.0.2.1"));
    }

    @Test
    void findAvailablePort_whenOnlyPortInRangeIsBound_returnsZero() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            int p = ss.getLocalPort();
            assertEquals(0, Net.findAvailablePort("127.0.0.1", p, p));
        }
    }

    @Test
    void waitUntilPortListening_zeroTimeoutThrowsImmediately() {
        assertThrows(IOException.class, () -> Net.waitUntilPortListening("127.0.0.1", 1, 0));
    }

    @Test
    void download_fileUrl_writesBytes(@TempDir Path dir) throws Exception {
        Path src = dir.resolve("src.bin");
        byte[] payload = new byte[]{0x01, 0x02, (byte) 0xFF};
        Files.write(src, payload);
        String url = src.toUri().toURL().toString();
        Path out = dir.resolve("out.bin");
        Net.download(url, out.toString());
        assertEquals(payload.length, Files.size(out));
    }
}
