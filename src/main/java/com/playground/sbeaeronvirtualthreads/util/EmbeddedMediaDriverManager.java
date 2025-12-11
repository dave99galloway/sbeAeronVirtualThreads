package com.playground.sbeaeronvirtualthreads.util;

import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages embedded Aeron Media Driver for testing
 */
public class EmbeddedMediaDriverManager {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedMediaDriverManager.class);
    private static MediaDriver mediaDriver;
    private static int referenceCount = 0;
    
    /**
     * Start the embedded media driver if not already running
     */
    public static synchronized void start() {
        if (referenceCount == 0) {
            MediaDriver.Context context = new MediaDriver.Context()
                .threadingMode(ThreadingMode.SHARED)
                .dirDeleteOnStart(true)
                .dirDeleteOnShutdown(true);
            
            mediaDriver = MediaDriver.launch(context);
            log.info("Embedded Media Driver started");
            
            // Give the driver a moment to initialize
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        referenceCount++;
        log.debug("Media Driver reference count: {}", referenceCount);
    }
    
    /**
     * Stop the embedded media driver when no longer needed
     */
    public static synchronized void stop() {
        referenceCount--;
        if (referenceCount == 0 && mediaDriver != null) {
            mediaDriver.close();
            mediaDriver = null;
            log.info("Embedded Media Driver stopped");
        }
    }
    
    /**
     * Force stop the media driver regardless of reference count
     */
    public static synchronized void forceStop() {
        if (mediaDriver != null) {
            mediaDriver.close();
            mediaDriver = null;
            referenceCount = 0;
            log.info("Embedded Media Driver force stopped");
        }
    }
    
    /**
     * Check if the media driver is running
     */
    public static synchronized boolean isRunning() {
        return mediaDriver != null;
    }
}
