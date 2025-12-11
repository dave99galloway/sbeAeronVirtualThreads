package com.playground.sbeaeronvirtualthreads.aeron;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aeron subscriber for receiving messages
 */
public class AeronSubscriber implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(AeronSubscriber.class);
    
    private final Aeron aeron;
    private final Subscription subscription;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong messagesReceived = new AtomicLong(0);
    private Thread pollingThread;
    
    public AeronSubscriber(String channel, int streamId) {
        this.aeron = Aeron.connect();
        this.subscription = aeron.addSubscription(channel, streamId);
        
        log.info("AeronSubscriber created on channel: {} streamId: {}", channel, streamId);
    }
    
    /**
     * Start polling for messages in a background thread
     */
    public void startPolling(FragmentHandler fragmentHandler) {
        if (running.compareAndSet(false, true)) {
            pollingThread = Thread.ofPlatform().name("aeron-subscriber").start(() -> {
                IdleStrategy idleStrategy = new SleepingIdleStrategy(1000);
                
                while (running.get()) {
                    int fragmentsRead = subscription.poll(fragmentHandler, 10);
                    messagesReceived.addAndGet(fragmentsRead);
                    idleStrategy.idle(fragmentsRead);
                }
            });
            
            log.info("Started polling thread");
        }
    }
    
    /**
     * Start polling with virtual thread
     */
    public void startPollingWithVirtualThread(FragmentHandler fragmentHandler) {
        if (running.compareAndSet(false, true)) {
            pollingThread = Thread.ofVirtual().name("aeron-subscriber-virtual").start(() -> {
                IdleStrategy idleStrategy = new SleepingIdleStrategy(1000);
                
                while (running.get()) {
                    int fragmentsRead = subscription.poll(fragmentHandler, 10);
                    messagesReceived.addAndGet(fragmentsRead);
                    idleStrategy.idle(fragmentsRead);
                }
            });
            
            log.info("Started polling virtual thread");
        }
    }
    
    /**
     * Poll once synchronously
     */
    public int pollOnce(FragmentHandler fragmentHandler, int fragmentLimit) {
        int fragmentsRead = subscription.poll(fragmentHandler, fragmentLimit);
        messagesReceived.addAndGet(fragmentsRead);
        return fragmentsRead;
    }
    
    /**
     * Stop polling
     */
    public void stopPolling() {
        if (running.compareAndSet(true, false)) {
            if (pollingThread != null) {
                try {
                    pollingThread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            log.info("Stopped polling thread");
        }
    }
    
    public long getMessagesReceived() {
        return messagesReceived.get();
    }
    
    public void resetMessageCount() {
        messagesReceived.set(0);
    }
    
    public boolean isConnected() {
        return subscription.isConnected();
    }
    
    public boolean hasImages() {
        return subscription.imageCount() > 0;
    }
    
    @Override
    public void close() {
        stopPolling();
        subscription.close();
        aeron.close();
        log.info("AeronSubscriber closed");
    }
}
