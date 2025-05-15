// 2025-05-15
// Java 21: Drag & Drop File- und Verzeichnis-Upload mit relativer Struktur via HTTP PUT

package com.example.sender;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;

public class FileDropUploader {

    private static final String TARGET_URL = "http://192.168.1.42:8080/upload/"; // Ziel-URL anpassen

    public static void main(String[] args) {
        var frame = new JFrame("Datei- & Ordner-Uploader per Drag & Drop");
        var label = new JLabel("<html><center>Dateien oder Ordner hierher ziehen<br>und sie werden rekursiv per HTTP PUT gesendet</center></html>", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 16));
        frame.add(label);

        new DropTarget(label, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    var transferable = dtde.getTransferable();
                    @SuppressWarnings("unchecked")
                    List<File> dropped = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    for (var file : dropped) {
                        if (file.isDirectory()) {
                            uploadDirectory(file.toPath());
                        } else {
                            uploadFile(file.toPath(), file.getParentFile().toPath());
                        }
                    }

                    JOptionPane.showMessageDialog(frame, "✅ Alle Dateien wurden übertragen.");

                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "❌ Fehler: " + e.getMessage());
                }
            }
        });

        frame.setSize(480, 220);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void uploadDirectory(Path baseDir) throws IOException {
        Files.walk(baseDir)
             .filter(Files::isRegularFile)
             .forEach(path -> {
                 try {
                     uploadFile(path, baseDir);
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
             });
    }

    private static void uploadFile(Path filePath, Path baseDir) throws IOException, InterruptedException {
        var relativePath = baseDir.relativize(filePath);
        var encodedPath = encodePath(relativePath);

        URI uri = URI.create(TARGET_URL + encodedPath);

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .PUT(HttpRequest.BodyPublishers.ofFile(filePath))
                .header("Content-Type", "application/octet-stream")
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("⬆ " + relativePath + " → " + response.statusCode());
    }

    private static String encodePath(Path path) {
        StringBuilder encoded = new StringBuilder();
        for (Path part : path) {
            if (encoded.length() > 0) encoded.append("/");
            encoded.append(URLEncoder.encode(part.toString(), StandardCharsets.UTF_8)
                                     .replace("+", "%20"));
        }
        return encoded.toString();
    }
}
