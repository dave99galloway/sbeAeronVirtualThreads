package com.playground.sbeaeronvirtualthreads.aeron;

import io.aeron.Aeron;
import io.aeron.Publication;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Aeron publisher for sending messages
 */
public class AeronPublisher implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AeronPublisher.class);
    
    private final Aeron aeron;
    private final Publication publication;
    private final UnsafeBuffer buffer;
    
    public AeronPublisher(String channel, int streamId, int bufferSize) {
        this.aeron = Aeron.connect();
        this.publication = aeron.addPublication(channel, streamId);
        this.buffer = new UnsafeBuffer(ByteBuffer.allocateDirect(bufferSize));
        
        log.info("AeronPublisher created on channel: {} streamId: {}", channel, streamId);
    }
    
    /**
     * Publish a message
     * @param data the data to publish
     * @param offset the offset in the data
     * @param length the length of the data
     * @return true if published successfully, false otherwise
     */
    public boolean publish(byte[] data, int offset, int length) {
        buffer.putBytes(0, data, offset, length);
        long result = publication.offer(buffer, 0, length);
        
        if (result > 0) {
            return true;
        } else if (result == Publication.BACK_PRESSURED) {
            log.debug("Back pressured");
            return false;
        } else if (result == Publication.NOT_CONNECTED) {
            log.warn("Not connected");
            return false;
        } else if (result == Publication.ADMIN_ACTION) {
            log.debug("Admin action");
            return false;
        } else if (result == Publication.CLOSED) {
            log.error("Publication closed");
            return false;
        } else if (result == Publication.MAX_POSITION_EXCEEDED) {
            log.error("Max position exceeded");
            return false;
        }
        
        return false;
    }
    
    /**
     * Publish with retry on back pressure
     */
    public void publishWithRetry(byte[] data, int offset, int length) {
        buffer.putBytes(0, data, offset, length);
        
        long result;
        while ((result = publication.offer(buffer, 0, length)) < 0) {
            if (result == Publication.BACK_PRESSURED) {
                Thread.onSpinWait();
            } else if (result == Publication.NOT_CONNECTED || 
                       result == Publication.CLOSED || 
                       result == Publication.MAX_POSITION_EXCEEDED) {
                throw new RuntimeException("Publication failed with result: " + result);
            }
        }
    }
    
    /**
     * Publish data already in the buffer with retry on back pressure
     */
    public void publish(int length) {
        long result;
        while ((result = publication.offer(buffer, 0, length)) < 0) {
            if (result == Publication.BACK_PRESSURED) {
                Thread.onSpinWait();
            } else if (result == Publication.NOT_CONNECTED || 
                       result == Publication.CLOSED || 
                       result == Publication.MAX_POSITION_EXCEEDED) {
                throw new RuntimeException("Publication failed with result: " + result);
            }
        }
    }
    
    public UnsafeBuffer getBuffer() {
        return buffer;
    }
    
    public boolean isConnected() {
        return publication.isConnected();
    }
    
    @Override
    public void close() {
        publication.close();
        aeron.close();
        log.info("AeronPublisher closed");
    }
}
