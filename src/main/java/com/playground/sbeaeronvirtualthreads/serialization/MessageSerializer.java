package com.playground.sbeaeronvirtualthreads.serialization;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * Interface for serializing and deserializing messages
 */
public interface MessageSerializer<T> {
    /**
     * Serialize a message into the buffer
     * @param message the message to serialize
     * @param buffer the buffer to write to
     * @param offset the offset in the buffer
     * @return the number of bytes written
     */
    int serialize(T message, MutableDirectBuffer buffer, int offset);
    
    /**
     * Deserialize a message from the buffer
     * @param buffer the buffer to read from
     * @param offset the offset in the buffer
     * @param length the length of the message
     * @return the deserialized message
     */
    T deserialize(DirectBuffer buffer, int offset, int length);
    
    /**
     * Get the name of this serialization format
     */
    String getFormatName();
}
