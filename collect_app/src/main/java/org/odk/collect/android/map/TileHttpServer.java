package org.odk.collect.android.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

/** A minimal HTTP server that serves tiles from a set of TileSources. */
public class TileHttpServer {
    final Map<String, TileSource> sources = new HashMap<>();
    final int port;
    ServerThread server;

    public TileHttpServer(int port) throws IOException {
        this.port = port;
        server = new ServerThread(port);
    }

    public void addSource(String key, TileSource source) {
        sources.put(key, source);
    }

    public void start() {
        if (!server.isAlive()) {
            server.start();
        }
    }

    public void stop() {
        if (server.isAlive()) {
            server.close();
            server.interrupt();
        }
    }

    class ServerThread extends Thread {
        int port;
        ServerSocket socket;

        public ServerThread(int port) throws IOException {
            super();
            this.port = port;
        }

        public void run() {
            try {
                socket = new ServerSocket(port);
                socket.setReuseAddress(true);
                Timber.i("Ready for requests on port %d", port);
                while (!isInterrupted()) {
                    Socket connection = socket.accept();
                    Timber.i("Accepted a client connection");
                    new ReplyThread(connection).start();
                }
            } catch (IOException e) {
                Timber.i("Server thread stopped: %s", e.getMessage());
            } finally {
                close();
            }
        }

        public void close() {
            if (socket != null) {
                try {
                    socket.close();  // makes socket.accept() throw SocketException
                } catch (IOException e) { /* ignore */ }
            }
        }
    }

    class ReplyThread extends Thread {
        final Socket connection;

        public ReplyThread(Socket connection) {
            this.connection = connection;
        }

        public void run() {
            try (Socket connection = this.connection) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                String request = new BufferedReader(reader).readLine();
                Timber.i("Received request: %s", request);
                long start = System.currentTimeMillis();
                byte[] data = getReply(request);
                sendReply(connection, data);
                long finish = System.currentTimeMillis();
                Timber.i("%s: Served %d bytes in %d ms", request, data.length, finish - start);
            } catch (IOException e) {
                Timber.e(e, "Unable to read request from socket");
            }
        }

        protected byte[] getReply(String request) {
            if (request.startsWith("GET /")) {
                String path = request.substring(5).split(" ", 2)[0];
                String[] parts = path.split("/");
                if (parts.length == 4) {
                    try {
                        String key = parts[0];
                        int zoom = Integer.parseInt(parts[1]);
                        int x = Integer.parseInt(parts[2]);
                        int y = Integer.parseInt(parts[3]);
                        TileSource source = sources.get(key);
                        if (source != null) {
                            return source.getTile(zoom, x, y);
                        }
                    } catch (NumberFormatException e) { /* ignore */ }
                }
            }
            Timber.w("Ignoring request: %s", request);
            return new byte[0];
        }

        protected void sendReply(Socket connection, byte[] data) {
            String headers = String.format(
                "HTTP/1.0 200\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-Length: %d\r\n" +
                    "\r\n",
                data.length
            );

            try (OutputStream output = connection.getOutputStream()) {
                output.write(headers.getBytes());
                output.write(data);
                output.flush();
            } catch (IOException e) {
                Timber.e(e, "Unable to write reply to socket");
            }
        }
    }

    public interface TileSource {
        byte[] getTile(int zoom, int x, int y);
    }
}
