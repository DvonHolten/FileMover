// 2025-05-15
// Java 21: Drag & Drop File- und Verzeichnis-Upload mit relativer Struktur via HTTP PUT

package com.example.sender;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileDropUploader {

    private static final String TARGET_URL = "http://192.168.0.20:8080/upload/"; // Ziel-URL anpassen

    private static final Font labelFont = new Font("SansSerif", Font.PLAIN, 16);

    public static void main(String[] args) {
        JFrame frame = new JFrame("Datei- & Ordner-Uploader per Drag & Drop");
        JLabel label = new JLabel("<html><center>Dateien oder Ordner hierher ziehen<br>und sie werden rekursiv per HTTP PUT gesendet</center></html>", SwingConstants.CENTER);
        label.setFont(labelFont);
        frame.add(label);

        new DropTarget(label, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    @SuppressWarnings("unchecked")
                    List<File> dropped = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    for (var file : dropped) {
                        if (file.isDirectory()) {
                            uploadDirectory(file.toPath());
                        } else {
                            uploadFile(file.toPath(), file.getParentFile().toPath());
                        }
                    }

                    // JOptionPane.showMessageDialog(frame, "? Alle Dateien wurden übertragen.");
                    JOptionPane.showMessageDialog(frame, "Alle Dateien wurden übertragen.");
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Fehler: " + e.getMessage());
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
        Path relativePath = baseDir.relativize(filePath);
        String encodedPath = encodePath(relativePath);
        long lastModified = relativePath.toFile().lastModified();
        URI uri = URI.create(TARGET_URL + encodedPath);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header( "X-Last-Modified", String.valueOf(lastModified) )
                .PUT(HttpRequest.BodyPublishers.ofFile(filePath))
                .header("Content-Type", "application/octet-stream")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("\u2B06 " + relativePath + " \u2192 " + response.statusCode());
    }

    private static String encodePath(Path path) {
        StringBuilder encoded = new StringBuilder();
        for (Path part : path) {
            if (!encoded.isEmpty()) encoded.append("/");
            encoded.append(URLEncoder.encode(part.toString(), StandardCharsets.UTF_8)
                                     .replace("+", "%20"));
        }
        return encoded.toString();
    }
}
