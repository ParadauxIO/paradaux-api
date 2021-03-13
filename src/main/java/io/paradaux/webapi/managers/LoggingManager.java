package io.paradaux.webapi.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingManager {

    private static final Logger logger = LoggerFactory.getLogger(LoggingManager.class);

    public static Logger getLogger() {
        return logger;
    }
}
