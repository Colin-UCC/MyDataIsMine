package com.fyp.mydataismine.auth;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.fyp.mydataismine.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ForgotPasswordTest {

    @Rule
    public ActivityScenarioRule<ForgotPassword> activityRule = new ActivityScenarioRule<>(ForgotPassword.class);

    @Test
    public void resetPassword_withEmptyEmail_showsError() {
        onView(withId(R.id.resetPasswordButton)).perform(click());
        onView(withText(R.string.error_field_required)).check(matches(isDisplayed()));
    }

    @Test
    public void resetPassword_withInvalidEmail_showsError() {
        String invalidEmail = "invalid";

        onView(withId(R.id.forgotEmailEditText)).perform(typeText(invalidEmail));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.resetPasswordButton)).perform(click());
        onView(withText(R.string.error_invalid_email)).check(matches(isDisplayed()));
    }

    @Test
    public void resetPassword_withValidEmail_showsSuccessMessage() {
        String validEmail = "test1@test.com";

        onView(withId(R.id.forgotEmailEditText)).perform(typeText(validEmail), closeSoftKeyboard());
        onView(withId(R.id.resetPasswordButton)).perform(click());

        onView(withId(R.id.messageTextView)).check(matches(withText("Check your email to reset your password")));
    }


//    @Test
//    public void resetPassword_withValidEmail_showsSuccessMessage() {
//        onView(withId(R.id.forgotEmailEditText)).perform(typeText("validemail@example.com"), ViewActions.closeSoftKeyboard());
//        onView(withId(R.id.resetPasswordButton)).perform(click());
//        onView(withText("Check your email to reset your password")).check(matches(isDisplayed()));
//    }
}
