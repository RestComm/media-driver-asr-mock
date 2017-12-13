/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-${YEAR}, Telestax Inc and individual contributors
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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Test;
import org.restcomm.media.drivers.asr.AsrDriverEventListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) created on 12/12/2017
 */
public class MockAsrDriverTest {

    private ListeningScheduledExecutorService scheduler;

    @After
    public void after() {
        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
            this.scheduler = null;
        }
    }

    @Test
    public void testFinalResultOnTimeout() {
        // given
        final int timeout = 100;

        final HashMap<String, String> parameters = new HashMap<>(1);
        parameters.put(MockAsrDriverParameter.TIMEOUT.symbol(), String.valueOf(timeout));

        this.scheduler = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        final MockAsrDriver driver = new MockAsrDriver(scheduler);

        final AsrDriverEventListener listener = mock(AsrDriverEventListener.class);

        // when
        driver.configure(parameters);
        driver.setListener(listener);
        driver.startRecognizing("", Collections.<String>emptyList());
        driver.write(new byte[10], 0, 10);
        driver.write(new byte[10], 0, 10);
        driver.write(new byte[10], 0, 10);

        // then
        verify(listener, timeout(timeout)).onSpeechRecognized(MockAsrDriver.TRANSCRIPTION_FINAL, true);
        assertEquals(10 * 3, driver.getOctetCount());
        assertEquals(3, driver.getWriteCount());
    }


}
