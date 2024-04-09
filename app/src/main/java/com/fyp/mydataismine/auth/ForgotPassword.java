package com.fyp.mydataismine.auth;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fyp.mydataismine.R;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Manages the password reset process for users who have forgotten their passwords.
 * This class interfaces with Firebase Authentication to send password reset emails to users.
 */
public class ForgotPassword extends AppCompatActivity {

    // UI Components
    private EditText forgotEmailEditText;
    private TextView errorMessageTextView;
    private TextView messageTextView;
    private Button resetPasswordButton, backButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    /**
     * Called when the activity is starting. Initialises the activity and UI components.
     * @param savedInstanceState If the activity is being re-initialised after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initializeUI();
        setupListeners();
    }

    /**
     * Initializes the user interface components and Firebase Authentication instance.
     */
    private void initializeUI() {
        mAuth = FirebaseAuth.getInstance();
        forgotEmailEditText = findViewById(R.id.forgotEmailEditText);
        resetPasswordButton = findViewById(R.id.resetPasswordButton);
        errorMessageTextView = findViewById(R.id.errorMessageTextView);
        backButton = findViewById(R.id.backButton);
        progressBar = findViewById(R.id.progressBar);
        messageTextView = findViewById(R.id.messageTextView);
    }

    /**
     * Sets up event listeners for the UI components.
     */
    private void setupListeners() {
        resetPasswordButton.setOnClickListener(v -> resetPassword());
        backButton.setOnClickListener(v -> finish()); // Finish activity and return to the previous screen
    }

    /**
     * Handles the password reset process by sending a password reset email to the user.
     */
    private void resetPassword() {
        String email = forgotEmailEditText.getText().toString().trim();

        if (!validateEmail(email)) return;

        progressBar.setVisibility(View.VISIBLE);
        messageTextView.setVisibility(View.GONE); // Hide previous messages
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                messageTextView.setText("Check your email to reset your password");
            } else {
                messageTextView.setText("Try again. Something wrong happened!");
            }
            messageTextView.setVisibility(View.VISIBLE); // Show the message
        });
    }

    private void displayErrorMessage(String message) {
        errorMessageTextView.setText(message);
        errorMessageTextView.setVisibility(View.VISIBLE);
    }

    /**
     * Validates the provided email address.
     * @param email The email address to validate.
     * @return true if the email address is valid, false otherwise.
     */
    boolean validateEmail(String email) {
        if (email.isEmpty()) {
            displayErrorMessage("Email is required");
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            displayErrorMessage("Please provide a valid email");
            return false;
        }

        errorMessageTextView.setVisibility(View.GONE);
        return true;
    }

}