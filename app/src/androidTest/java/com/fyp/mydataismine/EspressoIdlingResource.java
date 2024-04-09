package com.fyp.mydataismine;

import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

public class EspressoIdlingResource {

    private static final String RESOURCE = "GLOBAL";

    private static final CountingIdlingResource idlingResource =
            new CountingIdlingResource(RESOURCE);

    public static void increment() {
        idlingResource.increment();
    }

    public static void decrement() {
        if (!idlingResource.isIdleNow()) {
            idlingResource.decrement();
        }
    }

    public static IdlingResource getIdlingResource() {
        return idlingResource;
    }
}