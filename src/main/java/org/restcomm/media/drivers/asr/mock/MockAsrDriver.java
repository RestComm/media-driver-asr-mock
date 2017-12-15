/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.drivers.asr.mock;

import com.google.common.util.concurrent.*;
import org.apache.log4j.Logger;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;
import org.restcomm.media.drivers.asr.AsrDriverException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ASR mock driver that can be used for testing purposes.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MockAsrDriver implements AsrDriver {

    private static final Logger log = Logger.getLogger(MockAsrDriver.class);

    private static final int DEFAULT_TIMEOUT = 10000;
    static final String TRANSCRIPTION_FINAL = "Final transcription.";

    // Dependencies
    private final ListeningScheduledExecutorService scheduler = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(2));

    // Driver State
    private AsrDriverEventListener eventListener;
    private ListenableScheduledFuture<String> future;

    private final AtomicBoolean running;
    private int timeout;

    private int octetCount;
    private int writeCount;

    public MockAsrDriver(ListeningScheduledExecutorService scheduler) {
        // Driver State
        this.running = new AtomicBoolean(false);
        this.timeout = DEFAULT_TIMEOUT;
    }

    public MockAsrDriver() {
        this(MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor()));
    }

    @Override
    public void configure(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            MockAsrDriverParameter parameter = MockAsrDriverParameter.fromSymbol(entry.getKey());
            if (parameter != null) {
                switch (parameter) {
                    case TIMEOUT:
                        this.timeout = Integer.parseInt(entry.getValue());
                        if (log.isDebugEnabled()) {
                            log.debug("TIMEOUT is configured to " + this.timeout);
                        }
                        break;

                    default:
                        log.warn("Unknown parameter: " + entry.getKey());
                        break;
                }
            }
        }
    }

    @Override
    public void startRecognizing(String s, List<String> list) {
        if (this.running.compareAndSet(false, true)) {
            this.future = this.scheduler.schedule(new TranscriptionTask(), this.timeout, TimeUnit.MILLISECONDS);
            Futures.addCallback(future, new FutureCallback<String>() {

                @Override
                public void onSuccess(String result) {
                    if (MockAsrDriver.this.eventListener != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("ASR Operation successful! Result: " + result + ". Final: " + true);
                        }
                        MockAsrDriver.this.eventListener.onSpeechRecognized(result, true);
                        scheduler.shutdown();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    if (t instanceof CancellationException) {
                        log.warn("Operation was canceled.");
                    } else if (MockAsrDriver.this.eventListener != null) {
                        log.error("ASR Operation Failed", t);
                        MockAsrDriver.this.eventListener.onError(new AsrDriverException(t));
                    }
                    scheduler.shutdown();
                }
            });

            if (log.isDebugEnabled()) {
                log.debug("Started recognition process. Result will be received in " + this.timeout + "ms.");
            }
        } else {
            throw new IllegalStateException("ASR Driver is already running.");
        }
    }

    @Override
    public void finishRecognizing() {
        if (this.running.compareAndSet(true, false)) {
            if (log.isDebugEnabled()) {
                log.debug("Canceling future transcription task.");
            }
            this.future.cancel(false);
        } else {
            throw new IllegalStateException("ASR Driver is already stopped.");
        }
    }

    @Override
    public void write(byte[] bytes, int i, int i1) {
        if (this.running.get()) {
            this.octetCount += i1 - i;
            this.writeCount++;
        }
    }

    @Override
    public void write(byte[] bytes) {
        write(bytes, 0, bytes.length);
    }

    @Override
    public void setListener(AsrDriverEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public int getResponseTimeoutInMilliseconds() {
        return 1000;
    }

    int getOctetCount() {
        return octetCount;
    }

    int getWriteCount() {
        return writeCount;
    }

    private final class TranscriptionTask implements Callable<String> {

        @Override
        public String call() throws Exception {
            return TRANSCRIPTION_FINAL;
        }

    }


}
