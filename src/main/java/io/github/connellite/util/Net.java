package io.github.connellite.util;

import lombok.experimental.UtilityClass;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Small helpers for name resolution and streaming a URL to a local file.
 */
@UtilityClass
public class Net {
    private static final int BUFFER_SIZE = 1024;

    /**
     * Resolves a hostname or literal IP to the primary numeric address (IPv4 or IPv6 string).
     *
     * @param host hostname or address accepted by {@link InetAddress#getByName(String)}
     * @return {@link InetAddress#getHostAddress()}
     * @throws UnknownHostException if the name cannot be resolved
     */
    public static String address2Host(String host) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(host);
        return inetAddress.getHostAddress();
    }

    /**
     * Performs a reverse lookup for a numeric IP or resolves a hostname to its canonical host name.
     *
     * @param ip hostname or address accepted by {@link InetAddress#getByName(String)}
     * @return {@link InetAddress#getHostName()}
     * @throws UnknownHostException if the name cannot be resolved
     */
    public static String host2Address(String ip) throws UnknownHostException {
        InetAddress inetAddress = InetAddress.getByName(ip);
        return inetAddress.getHostName();
    }

    /**
     * Downloads the response body of {@code fileUrl} into {@code fileName} (overwrites if it exists).
     *
     * @param fileUrl  HTTP/HTTPS URL (or other scheme supported by {@link URL#openStream()})
     * @param fileName destination path on the local filesystem
     * @throws IOException if the connection or file write fails
     */
    public static void download(String fileUrl, String fileName) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new URL(fileUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
            byte[] dataBuffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }

    /**
     * Finds an available port within a port range on a host
     *
     * @param hostname  Host name
     * @param startPort Port number starting the range
     * @param endPort   Port number ending the range
     * @return An available port number, or 0 if none is available.
     */
    public static int findAvailablePort(String hostname, int startPort, int endPort) {
        for (int port = startPort; port < (endPort + 1); port++) {
            try {
                Socket socket = new Socket(InetAddress.getByName(hostname), port);
                // port not available
                socket.close();
            } catch (IOException e) {
                // port available
                return port;
            }
        }

        return 0;
    }

    /**
     * Waits until a port is listening on a given host. An exception is thrown
     * if the timeout is excedeed.
     *
     * @param hostname Host name
     * @param port     Port number
     * @param timeout  Timeout in seconds
     * @throws IOException If a connection error occurs or if the timeout is exceeded
     */
    public static void waitUntilPortListening(String hostname, int port, int timeout) throws IOException {
        int i = 0;
        while (i < timeout) {
            // try to get connection
            try {
                Socket socket = new Socket(InetAddress.getByName(hostname), port);
                // connection OK: exit
                socket.close();
                return;
            } catch (IOException e) {
                // nothing
            }

            i++;

            // wait for 1 second
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // nothing
            }
        }

        throw new IOException("Timeout waiting for port " + port + " to listen");
    }
}
