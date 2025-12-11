package com.playground.sbeaeronvirtualthreads.util;

import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages embedded Aeron Media Driver for the application
 */
public class EmbeddedMediaDriver {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedMediaDriver.class);
    private final MediaDriver mediaDriver;
    
    public EmbeddedMediaDriver() {
        MediaDriver.Context context = new MediaDriver.Context()
            .threadingMode(ThreadingMode.SHARED)
            .dirDeleteOnStart(true)
            .dirDeleteOnShutdown(true);
        
        this.mediaDriver = MediaDriver.launch(context);
        log.info("Embedded Media Driver started");
    }
    
    public void close() {
        if (mediaDriver != null) {
            mediaDriver.close();
            log.info("Embedded Media Driver stopped");
        }
    }
}
