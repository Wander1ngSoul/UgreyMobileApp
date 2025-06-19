package com.example.ugreymobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private static final int MAX_INPUT_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}-]+$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z]).{8,}$");

    private TextInputEditText etLastName, etFirstName, etMiddleName, etEmail, etPassword, etConfirmPassword;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("users");

        etLastName = findViewById(R.id.etLastName);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        setupNameField(etLastName);
        setupNameField(etFirstName);
        setupNameField(etMiddleName);

        findViewById(R.id.btnRegister).setOnClickListener(v -> registerUser());
    }

    private void setupNameField(final TextInputEditText field) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(" ")) {
                    String filtered = s.toString().replaceAll(" ", "");
                    field.setText(filtered);
                    field.setSelection(filtered.length());
                    field.setError("Пробелы не допускаются");
                }

                if (s.length() > MAX_INPUT_LENGTH) {
                    field.setError("Максимум " + MAX_INPUT_LENGTH + " символов");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    public boolean isEmailValid(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isPasswordValid(String password) {
        return PASSWORD_PATTERN.matcher(password).matches() &&
                password.length() >= 8;
    }

    private void registerUser() {
        String lastName = etLastName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Введите фамилию");
            return;
        }
        if (!NAME_PATTERN.matcher(lastName).matches()) {
            etLastName.setError("Только буквы и дефисы, без пробелов");
            return;
        }
        if (lastName.length() > MAX_INPUT_LENGTH) {
            etLastName.setError("Максимум " + MAX_INPUT_LENGTH + " символов");
            return;
        }

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("Введите имя");
            return;
        }
        if (!NAME_PATTERN.matcher(firstName).matches()) {
            etFirstName.setError("Только буквы и дефисы, без пробелов");
            return;
        }
        if (firstName.length() > MAX_INPUT_LENGTH) {
            etFirstName.setError("Максимум " + MAX_INPUT_LENGTH + " символов");
            return;
        }

        if (!TextUtils.isEmpty(middleName)) {
            if (!NAME_PATTERN.matcher(middleName).matches()) {
                etMiddleName.setError("Только буквы и дефисы, без пробелов");
                return;
            }
            if (middleName.length() > MAX_INPUT_LENGTH) {
                etMiddleName.setError("Максимум " + MAX_INPUT_LENGTH + " символов");
                return;
            }
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            return;
        }

        if (password.length() < 8) {
            etPassword.setError("Пароль должен содержать минимум 8 символов");
            return;
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            etPassword.setError("Пароль должен содержать хотя бы 1 букву");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Пароли не совпадают");
            return;
        }

        String passwordHash = hashPassword(password);

        databaseReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    etEmail.setError("Пользователь с таким email уже существует");
                } else {
                    User user = new User(lastName, firstName, middleName, email, passwordHash);

                    String userId = email.replace(".", ",");
                    databaseReference.child(userId).setValue(user)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegisterActivity.this, AuthActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(RegisterActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RegisterActivity.this, "Ошибка базы данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onLoginClick(View view) {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }
}