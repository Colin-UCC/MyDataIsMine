package com.fyp.mydataismine.auth;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.fyp.mydataismine.MainActivity;
import com.fyp.mydataismine.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Manages user authentication including sign-in and registration processes.
 * This class integrates with Firebase Authentication to provide a secure
 * and efficient way to handle user authentication and account management.
 */
public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private TextView errorTextView;
    private Button signInOrRegisterButton, forgotPasswordButton;
    private ToggleButton toggleButton;
    private boolean isSignInMode = true;

    /**
     * Initializes the activity, setting up the user interface and authentication components.
     * @param savedInstanceState Contains data supplied in onSaveInstanceState(Bundle) if the activity is being re-initialized.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeUI();
        checkCurrentUser();
    }

    /**
     * Initializes user interface elements and sets up event handlers.
     */
    private void initializeUI() {
        mAuth = FirebaseAuth.getInstance();
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInOrRegisterButton = findViewById(R.id.signInOrRegisterButton);
        toggleButton = findViewById(R.id.toggleButton);
        forgotPasswordButton = findViewById(R.id.forgotPasswordButton);
        errorTextView = findViewById(R.id.errorTextView);

        signInOrRegisterButton.setOnClickListener(this::handleSignInOrRegister);
        toggleButton.setOnClickListener(this::toggleSignInRegister);
        forgotPasswordButton.setOnClickListener(this::navigateToForgotPassword);

        updateUI();  // Ensure UI is updated to reflect the initial state
    }

    /**
     * Checks if a user is already logged in and navigates to the main activity if so.
     */
    private void checkCurrentUser() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        }
    }

    /**
     * Navigates to the main activity of the app.
     */
    private void navigateToMain() {
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    /**
     * Handles sign-in or registration based on the current mode when the button is clicked.
     * @param view The view that was clicked.
     */
    private void handleSignInOrRegister(View view) {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (isSignInMode) {
            signIn(email, password);
        } else {
            register(email, password);
        }
    }

    /**
     * Signs in the user with the provided email and password.
     * @param email User's email address.
     * @param password User's password.
     */
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                navigateToMain();
                errorTextView.setVisibility(View.GONE);
            } else {
                errorTextView.setText("Sign-in failed. Please check your credentials and try again.");
                errorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Registers a new user with the provided email and password.
     * @param email User's email address.
     * @param password User's password.
     */
    private void register(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            errorTextView.setText("Email and password cannot be empty.");
            errorTextView.setVisibility(View.VISIBLE);
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorTextView.setText("Enter a valid email address.");
            errorTextView.setVisibility(View.VISIBLE);
            return;
        }

        if (password.length() < 6) {
            errorTextView.setText("Password must be at least 6 characters long.");
            errorTextView.setVisibility(View.VISIBLE);
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                sendEmailVerification(mAuth.getCurrentUser());
                errorTextView.setVisibility(View.GONE);
            } else {
                errorTextView.setText("Registration failed.");
                errorTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Sends an email verification to the user's email address.
     * @param user The currently logged-in FirebaseUser object.
     */
    private void sendEmailVerification(FirebaseUser user) {
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * Toggles between sign-in and registration mode.
     * @param view The view that was clicked.
     */
    private void toggleSignInRegister(View view) {
        isSignInMode = !isSignInMode;
        updateUI();
    }

    /**
     * Navigates to the ForgotPassword activity.
     * @param view The view that was clicked.
     */
    private void navigateToForgotPassword(View view) {
        startActivity(new Intent(LoginActivity.this, ForgotPassword.class));
    }

    /**
     * Updates the user interface based on whether the user is in sign-in or registration mode.
     */
    private void updateUI() {
        if (isSignInMode) {
            signInOrRegisterButton.setText("Sign In");
            toggleButton.setTextOff("Switch to Registration");
        } else {
            signInOrRegisterButton.setText("Register");
            toggleButton.setTextOn("Switch to Sign-In");
        }
    }

    private boolean validateForm(String email, String password) {
        if (email.isEmpty()) {
            emailEditText.setError("Email cannot be empty.");
            return false;
        } else if (password.isEmpty()) {
            passwordEditText.setError("Password cannot be empty.");
            return false;
        }
        return true;
    }
}
