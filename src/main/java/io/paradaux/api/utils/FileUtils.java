package io.paradaux.api.utils;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    public static void downloadAndExtractZip(String url, Path extractDir) throws IOException {
        Request request = new Request.Builder()
                .url(url)
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
}
