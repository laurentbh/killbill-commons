/*
 * Copyright 2017 Groupon, Inc
 * Copyright 2017 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.google.common.eventbus;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class TestMultiThreadedEventBusThatThrowsException extends TestSetupEventBusThatThrowsException {

    @BeforeSuite(groups = "fast")
    public void setUpSuite() throws Exception {
        busSetup();
    }

    // Make sure to use a large enough invocationCount so that threads are re-used
    @Test(groups = "fast", threadPoolSize = 25, invocationCount = 100, description = "Check that postWithException is thread safe")
    public void testThrowFirstExceptionMultithreaded() throws Exception {
        final int n = rand.nextInt(10000) + 1;
        final String subscriberMarker = n % 2 == 0 ? "A" : "B";
        final MyEvent event = new MyEvent(UUID.randomUUID(), subscriberMarker);

        try {
            eventBus.postWithException(event);
            Assert.fail();
        } catch (final EventBusException e) {
            Assert.assertTrue(e.getCause() instanceof InvocationTargetException);
            Assert.assertTrue(e.getCause().getCause() instanceof RuntimeException);
            Assert.assertEquals(e.getCause().getCause().getMessage(), Subscriber.exceptionMarker(subscriberMarker));
        }

        if ("A".equals(subscriberMarker)) {
            checkEventsSeen(subscriberA);
            checkEventsSeen(subscriberB, event);
        } else {
            checkEventsSeen(subscriberA, event);
            checkEventsSeen(subscriberB);
        }
    }
}
