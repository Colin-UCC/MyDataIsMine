package com.fyp.mydataismine.auth;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static com.google.common.base.CharMatcher.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import static java.util.function.Predicate.not;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fyp.mydataismine.EspressoIdlingResource;
import com.fyp.mydataismine.MainActivity;
import com.fyp.mydataismine.R;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsTestRule = new IntentsTestRule<>(LoginActivity.class);

    private FirebaseAuth mockAuth;
    private FirebaseUser mockUser;

    @Before
    public void setUp() {
//        mockAuth = mock(FirebaseAuth.class);
//        mockUser = mock(FirebaseUser.class);
//
//        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
//        // Set up other mocks as necessary

        IdlingRegistry.getInstance().register(EspressoIdlingResource.getIdlingResource());

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.signInWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(Tasks.forResult(mock(AuthResult.class)));
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.getIdlingResource());
    }

//    @Test
//    public void signInWithValidCredentials_navigatesToMainActivity() {
//        String validEmail = "test@test.com";
//    String validPassword = "password";
//
//        onView(withId(R.id.emailEditText)).perform(typeText(validEmail));
//        onView(withId(R.id.passwordEditText)).perform(typeText(validPassword));
//        onView(withId(R.id.signInOrRegisterButton)).perform(click());
//
//        intended(hasComponent(MainActivity.class.getName()));
//    }

//    @Test
//    public void authenticationFailure_showsAppropriateErrorMessage() {
//        String invalidEmail = "wrong@example.com";
//        String invalidPassword = "wrongPass";
//
//        onView(withId(R.id.emailEditText)).perform(typeText(invalidEmail));
//        onView(withId(R.id.passwordEditText)).perform(typeText(invalidPassword));
//        onView(withId(R.id.signInOrRegisterButton)).perform(click());
//
//        onView(withText(R.string.login_failed)).check(matches(isDisplayed()));
//    }

    @Test
    public void forgotPasswordButton_navigatesToForgotPasswordActivity() {
        onView(withId(R.id.forgotPasswordButton)).perform(click());

        intended(hasComponent(ForgotPassword.class.getName()));
    }

    @Test
    public void toggleButton_switchesBetweenSignInAndRegister() {
        // Check if the initial state is for signing in
        onView(withId(R.id.signInOrRegisterButton)).check(matches(withText("Sign In")));

        // Click toggle button to switch to registration
        onView(withId(R.id.toggleButton)).perform(click());

        // Check if the state has changed to registration
        onView(withId(R.id.signInOrRegisterButton)).check(matches(withText("Register")));

        // Click toggle button again to switch back to sign-in
        onView(withId(R.id.toggleButton)).perform(click());

        // Check if the state has reverted to sign-in
        onView(withId(R.id.signInOrRegisterButton)).check(matches(withText("Sign In")));
    }
}

//@RunWith(AndroidJUnit4.class)
//public class LoginActivityTest {
//
//    @Rule
//    public IntentsTestRule<LoginActivity> intentsTestRule = new IntentsTestRule<>(LoginActivity.class);
//
//    @Rule
//    public ActivityScenarioRule<LoginActivity> activityRule = new ActivityScenarioRule<>(LoginActivity.class);
//
//    @Test
//    public void toggleSignInRegister_switchesMode() {
//        onView(withId(R.id.toggleButton)).perform(click());
//        onView(withId(R.id.signInOrRegisterButton)).check(matches(withText("Register")));
//
//        onView(withId(R.id.toggleButton)).perform(click());
//        onView(withId(R.id.signInOrRegisterButton)).check(matches(withText("Sign In")));
//    }
//
//    // Mock FirebaseAuth
//    private FirebaseAuth mockAuth;
//
//    @Before
//    public void setUp() {
//        mockAuth = Mockito.mock(FirebaseAuth.class);
//
//        // Mock the Task object
//        Task mockSignInTask = Mockito.mock(Task.class);
//        Task mockCreateUserTask = Mockito.mock(Task.class);
//
//        when(mockSignInTask.isSuccessful()).thenReturn(true);
//        when(mockCreateUserTask.isSuccessful()).thenReturn(true);
//
//        when(mockAuth.signInWithEmailAndPassword(anyString(), anyString())).thenReturn(mockSignInTask);
//        when(mockAuth.createUserWithEmailAndPassword(anyString(), anyString())).thenReturn(mockCreateUserTask);
//    }
//
//    @Test
//    public void signInWithValidCredentials_navigatesToMainActivity() {
//        // Assume these credentials are valid
//        String validEmail = "test1@test.com";
//        String validPassword = "password123";
//
//        onView(withId(R.id.emailEditText)).perform(typeText(validEmail));
//        onView(withId(R.id.passwordEditText)).perform(typeText(validPassword));
//        onView(withId(R.id.signInOrRegisterButton)).perform(click());
//
//        intended(hasComponent(MainActivity.class.getName()));
//    }
//
//    @Test
//    public void authenticationFailure_showsAppropriateErrorMessage() {
//        // Assume these credentials are invalid
//        String invalidEmail = "invalid@example.com";
//        String invalidPassword = "invalidPassword";
//
//        // Attempt to sign in with invalid credentials
//        onView(withId(R.id.emailEditText)).perform(typeText(invalidEmail));
//        onView(withId(R.id.passwordEditText)).perform(typeText(invalidPassword));
//        onView(withId(R.id.signInOrRegisterButton)).perform(click());
//        onView(withText(R.string.login_failed)).check(matches(isDisplayed()));
//
//
//        // Toggle to registration mode
//        onView(withId(R.id.toggleButton)).perform(click());
//
//        // Attempt to register with invalid credentials
//        onView(withId(R.id.emailEditText)).perform(typeText(invalidEmail));
//        onView(withId(R.id.passwordEditText)).perform(typeText(invalidPassword));
//        onView(withId(R.id.signInOrRegisterButton)).perform(click());
//        onView(withText(R.string.registration_failed)).check(matches(isDisplayed()));
//    }
//
//
//
//    @Test
//    public void forgotPasswordButton_navigatesToForgotPasswordActivity() {
//        onView(withId(R.id.forgotPasswordButton)).perform(click());
//        intended(hasComponent(ForgotPassword.class.getName()));
//    }
//
//}
