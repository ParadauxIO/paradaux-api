package io.paradaux.api.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public class FileUtils {
    private static final int BUFFER_SIZE = 8192; // 8KB buffer
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

    /**
     * Downloads a ZIP file from the given URL and extracts it to the specified directory.
     * If the ZIP contains a single root folder, its contents are flattened to the extract directory.
     *
     * @param url The URL to download from
     * @param extractDir The directory path where files should be extracted
     * @throws IOException if download or extraction fails
     */
    public static void downloadAndExtractZip(String url, Path extractDir, String username, String password) throws IOException {
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        log.info("Downloading ZIP from URL: {}", url);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", basicAuth)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Download failed: " + response.code() + " " + response.message());
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Response body is null");
            }

            // First pass: collect all entries to determine structure
            List<ZipEntry> entries = new ArrayList<>();
            String commonRoot = null;

            try (InputStream inputStream = body.byteStream();
                 BufferedInputStream bufferedInput = new BufferedInputStream(inputStream, BUFFER_SIZE);
                 ZipInputStream zipStream = new ZipInputStream(bufferedInput)) {

                ZipEntry entry;
                while ((entry = zipStream.getNextEntry()) != null) {
                    entries.add(new ZipEntry(entry));
                    zipStream.closeEntry();
                }
            }

            // Determine if there's a single root folder
            if (!entries.isEmpty()) {
                String firstEntryName = entries.get(0).getName();
                int firstSlash = firstEntryName.indexOf('/');

                if (firstSlash > 0) {
                    String potentialRoot = firstEntryName.substring(0, firstSlash + 1);
                    boolean allEntriesShareRoot = entries.stream()
                            .allMatch(e -> e.getName().startsWith(potentialRoot));

                    if (allEntriesShareRoot) {
                        commonRoot = potentialRoot;
                    }
                }
            }

            // Second pass: extract files
            try (InputStream inputStream2 = client.newCall(request).execute().body().byteStream();
                 BufferedInputStream bufferedInput2 = new BufferedInputStream(inputStream2, BUFFER_SIZE);
                 ZipInputStream zipStream2 = new ZipInputStream(bufferedInput2)) {

                ZipEntry entry;
                while ((entry = zipStream2.getNextEntry()) != null) {
                    String entryName = entry.getName();

                    // Remove common root if present
                    if (commonRoot != null && entryName.startsWith(commonRoot)) {
                        entryName = entryName.substring(commonRoot.length());
                    }

                    // Skip empty paths after root removal
                    if (entryName.isEmpty()) {
                        zipStream2.closeEntry();
                        continue;
                    }

                    if (entry.isDirectory()) {
                        // Create directory
                        Path dirPath = extractDir.resolve(entryName);
                        Files.createDirectories(dirPath);
                    } else {
                        // Extract file
                        Path filePath = extractDir.resolve(entryName);
                        Files.createDirectories(filePath.getParent());

                        try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                             BufferedOutputStream bos = new BufferedOutputStream(fos, BUFFER_SIZE)) {

                            byte[] buffer = new byte[BUFFER_SIZE];
                            int bytesRead;
                            while ((bytesRead = zipStream2.read(buffer)) != -1) {
                                bos.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                    zipStream2.closeEntry();
                }
            }
        }
    }

    public static void extractZip(InputStream inputStream, Path extractDir) throws IOException {
        List<ZipEntry> entries = new ArrayList<>();
        String commonRoot = null;

        try (BufferedInputStream bis = new BufferedInputStream(inputStream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            StreamUtils.copy(bis, baos);
            byte[] zipBytes = baos.toByteArray();

            try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zipStream.getNextEntry()) != null) {
                    entries.add(new ZipEntry(entry));
                    zipStream.closeEntry();
                }
            }

            if (!entries.isEmpty()) {
                String firstName = entries.get(0).getName();
                int slash = firstName.indexOf('/');
                if (slash > 0) {
                    String potentialRoot = firstName.substring(0, slash + 1);
                    boolean allShareRoot = entries.stream()
                            .allMatch(e -> e.getName().startsWith(potentialRoot));
                    if (allShareRoot) commonRoot = potentialRoot;
                }
            }

            try (ZipInputStream zipStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
                ZipEntry entry;
                while ((entry = zipStream.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (commonRoot != null && name.startsWith(commonRoot)) {
                        name = name.substring(commonRoot.length());
                    }

                    if (name.isEmpty()) {
                        zipStream.closeEntry();
                        continue;
                    }

                    Path targetPath = extractDir.resolve(name);
                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        try (OutputStream os = Files.newOutputStream(targetPath);
                             BufferedOutputStream bos = new BufferedOutputStream(os, BUFFER_SIZE)) {
                            byte[] buffer = new byte[BUFFER_SIZE];
                            int len;
                            while ((len = zipStream.read(buffer)) > 0) {
                                bos.write(buffer, 0, len);
                            }
                        }
                    }

                    zipStream.closeEntry();
                }
            }
        }
    }
}
