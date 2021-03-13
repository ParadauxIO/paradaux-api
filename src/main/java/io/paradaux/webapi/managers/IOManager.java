package io.paradaux.webapi.managers;

import com.google.gson.Gson;
import io.paradaux.webapi.models.Configuration;
import org.slf4j.Logger;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOManager {

    private final Logger logger = LoggingManager.getLogger();

    public IOManager() {

    }

    /**
     * Export a resource embedded into a Jar file to the local file path.
     *
     * @param inputPath ie.: "/configuration.yml" N.B / is a directory down in the "jar tree" been the jar the root of the tree
     * @throws FileNotFoundException a generic exception to signal something went wrong
     */
    public static void exportResource(String inputPath, @Nullable String outputPath) throws FileNotFoundException {

        OutputStream resourceOutputStream;

        try (InputStream resourceStream = IOManager.class.getResourceAsStream(inputPath)) {
            resourceOutputStream = new FileOutputStream(outputPath);

            if (resourceStream == null) {
                throw new FileNotFoundException("Cannot get resource \"" + inputPath + "\" from Jar file.");
            }

            int readBytes;
            byte[] buffer = new byte[4096];

            while ((readBytes = resourceStream.read(buffer)) > 0) {
                resourceOutputStream.write(buffer, 0, readBytes);
            }

            resourceOutputStream.close();
        } catch (IOException exception) {
            throw new FileNotFoundException("Failed to deploy resource : " + exception.getMessage());
        }
    }

    /**
     * Reads the configuration file and maps it to the ConfigurationEntry object.
     * @return An Instance of ConfigurationEntry
     * @throws FileNotFoundException When the configuration file does not exist.
     * @see Configuration
     * */
    public Configuration readConfigurationFile() throws FileNotFoundException {
        return new Gson().fromJson(new FileReader("config.json"), Configuration.class);
    }

    /**
     * Copies the configuration file from the JAR to the current directory on first run.
     */
    public static void deployFiles() {
        String jarLocation = System.getProperty("user.dir");

        if (!new File("config.json").exists()) {
            try {
                exportResource("/config.json", jarLocation + "/config.json");
            } catch (Exception exception) {
                LoggingManager.getLogger().error("Failed to deploy config.json\n", exception);
            }
        }
    }
}
