// 2025-05-15
// Java 21: Minimaler HTTP PUT Server zur Dateiannahme mit Pfadstruktur

package com.example.receiver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.*;

public class FileReceiverServer {

    private static final Path ROOT_DIR = Paths.get("uploads"); // Zielordner

    public static void main(String[] args) throws IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/upload/", new UploadHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("🚀 Empfänger läuft auf http://localhost:8080/upload/");
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            var rawPath = exchange.getRequestURI().getPath().substring("/upload/".length());
            var decodedPath = URLDecoder.decode(rawPath, "UTF-8");

            Path targetPath = ROOT_DIR.resolve(decodedPath).normalize();
            if (!targetPath.startsWith(ROOT_DIR)) {
                exchange.sendResponseHeaders(403, -1); // Forbidden
                return;
            }

            Files.createDirectories(targetPath.getParent());

            try (var out = Files.newOutputStream(targetPath);
                 var in = exchange.getRequestBody()) {
                in.transferTo(out);
            }

            String msg = "✅ Gespeichert: " + targetPath;
            System.out.println(msg);
            byte[] response = msg.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (var os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }
}
