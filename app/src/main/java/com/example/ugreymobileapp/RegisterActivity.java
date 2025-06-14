package com.example.ugreymobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class RegisterActivity extends AppCompatActivity {

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

        findViewById(R.id.btnRegister).setOnClickListener(v -> registerUser());
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

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("Введите имя");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Пароль должен содержать минимум 6 символов");
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