package com.fyp.mydataismine.packetcapture;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import com.fyp.mydataismine.R;

@RunWith(AndroidJUnit4.class)
public class PacketStreamActivityTest {

    @Rule
    public ActivityScenarioRule<PacketStreamActivity> activityRule = new ActivityScenarioRule<>(PacketStreamActivity.class);

    @Test
    public void testActivityLaunch() {
        ActivityScenario<PacketStreamActivity> scenario = activityRule.getScenario();
        Espresso.onView(withId(R.id.packetListView)).check(matches(isDisplayed()));
    }

}
