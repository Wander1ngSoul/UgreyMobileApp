package com.example.ugreymobileapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import java.util.regex.Pattern;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";
    private static final int MAX_INPUT_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}-]+$"); // Только буквы и дефисы

    private TextInputEditText etLastName, etFirstName, etMiddleName, etEmail;
    private Button btnEdit, btnSave, btnCancel, btnDelete;
    private DatabaseReference databaseReference;
    private String currentUserEmail;
    private String currentUserName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUserEmail = requireActivity().getIntent().getStringExtra("email");
        if (currentUserEmail == null) {
            redirectToAuth();
            return;
        }

        currentUserName = requireActivity().getIntent().getStringExtra("name");

        String userId = currentUserEmail.replace(".", ",");
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupValidation();
        return view;
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

        btnSave.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
    }

    private void setupValidation() {
        setupNameField(etLastName);
        setupNameField(etFirstName);
        setupNameField(etMiddleName);

        etEmail.setEnabled(false);
    }

    private void setupNameField(TextInputEditText field) {
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserData();
        setupButtonListeners();
    }

    private void loadUserData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        etLastName.setText(user.getLastName());
                        etFirstName.setText(user.getFirstName());
                        etMiddleName.setText(user.getMiddleName());
                        etEmail.setText(user.getEmail());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
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

        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Введите фамилию");
            return;
        }
        if (!NAME_PATTERN.matcher(lastName).matches()) {
            etLastName.setError("Только буквы и дефисы");
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
            etFirstName.setError("Только буквы и дефисы");
            return;
        }
        if (firstName.length() > MAX_INPUT_LENGTH) {
            etFirstName.setError("Максимум " + MAX_INPUT_LENGTH + " символов");
            return;
        }

        if (!TextUtils.isEmpty(middleName)) {
            if (!NAME_PATTERN.matcher(middleName).matches()) {
                etMiddleName.setError("Только буквы и дефисы");
                return;
            }
            if (middleName.length() > MAX_INPUT_LENGTH) {
                etMiddleName.setError("Максимум " + MAX_INPUT_LENGTH + " символов");
                return;
            }
        }

        // Обновление данных
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
                                                        showError("Ошибка при обновлении отчества");
                                                    }
                                                });
                                    } else {
                                        showError("Ошибка при обновлении имени");
                                    }
                                });
                    } else {
                        showError("Ошибка при обновлении фамилии");
                    }
                });
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(getContext(), "Ошибка удаления данных", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void redirectToAuth() {
        startActivity(new Intent(getActivity(), AuthActivity.class));
        requireActivity().finish();
    }
}