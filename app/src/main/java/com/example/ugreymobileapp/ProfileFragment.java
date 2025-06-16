package com.example.ugreymobileapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private TextInputEditText etLastName, etFirstName, etMiddleName, etEmail;
    private Button btnEdit, btnSave, btnCancel, btnDelete;
    private DatabaseReference databaseReference;
    private String currentUserEmail;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Получаем email из SharedPreferences или Activity
        currentUserEmail = getActivity().getIntent().getStringExtra("email");
        if (currentUserEmail == null) {
            redirectToAuth();
            return;
        }

        String userId = currentUserEmail.replace(".", ",");
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserData();
        setupButtonListeners();
    }

    private void initViews(View view) {
        etLastName = view.findViewById(R.id.etLastName);
        etFirstName = view.findViewById(R.id.etFirstName);
        etMiddleName = view.findViewById(R.id.etMiddleName);
        etEmail = view.findViewById(R.id.etEmail);

        btnEdit = view.findViewById(R.id.btnEdit);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnDelete = view.findViewById(R.id.btnDelete);

        // Начальное состояние кнопок
        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
    }

    private void loadUserData() {
        if (databaseReference == null) {
            Log.e(TAG, "Database reference is null");
            redirectToAuth();
            return;
        }

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.d(TAG, "No user data found in database");
                    Toast.makeText(getContext(), "Данные пользователя не найдены", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        etLastName.setText(user.getLastName());
                        etFirstName.setText(user.getFirstName());
                        etMiddleName.setText(user.getMiddleName());
                        etEmail.setText(user.getEmail());
                        Log.d(TAG, "User data loaded successfully");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing user data: " + e.getMessage());
                    Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
                Toast.makeText(getContext(), "Ошибка базы данных", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupButtonListeners() {
        btnEdit.setOnClickListener(v -> enableEditing());
        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> cancelEditing());
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void enableEditing() {
        etLastName.setEnabled(true);
        etFirstName.setEnabled(true);
        etMiddleName.setEnabled(true);

        btnSave.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);
    }

    private void saveChanges() {
        String lastName = etLastName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String middleName = etMiddleName.getText().toString().trim();

        if (lastName.isEmpty()) {
            etLastName.setError("Введите фамилию");
            return;
        }

        if (firstName.isEmpty()) {
            etFirstName.setError("Введите имя");
            return;
        }

        databaseReference.child("lastName").setValue(lastName)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        databaseReference.child("firstName").setValue(firstName)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        databaseReference.child("middleName").setValue(middleName)
                                                .addOnCompleteListener(task2 -> {
                                                    if (task2.isSuccessful()) {
                                                        Toast.makeText(getContext(), "Данные обновлены", Toast.LENGTH_SHORT).show();
                                                        disableEditing();
                                                    } else {
                                                        Toast.makeText(getContext(), "Ошибка при обновлении отчества", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(getContext(), "Ошибка при обновлении имени", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(getContext(), "Ошибка при обновлении фамилии", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cancelEditing() {
        disableEditing();
        loadUserData();
    }

    private void disableEditing() {
        etLastName.setEnabled(false);
        etFirstName.setEnabled(false);
        etMiddleName.setEnabled(false);

        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnEdit.setVisibility(View.VISIBLE);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Удаление аккаунта")
                .setMessage("Вы уверены, что хотите удалить свой аккаунт? Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> deleteAccount())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteAccount() {
        databaseReference.removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Аккаунт удален", Toast.LENGTH_SHORT).show();
                        redirectToAuth();
                    } else {
                        Toast.makeText(getContext(), "Ошибка удаления данных: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirectToAuth() {
        startActivity(new Intent(getContext(), AuthActivity.class));
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}