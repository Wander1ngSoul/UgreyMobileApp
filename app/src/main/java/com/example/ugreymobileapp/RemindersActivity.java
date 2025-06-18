package com.example.ugreymobileapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RemindersActivity extends AppCompatActivity {
    private EditText etTitle, etDescription, etDueDate;
    private Button btnAdd, btnUpdate, btnDelete, btnDatePicker;
    private ListView listView;

    private DatabaseReference databaseReference;
    private String currentUserId;
    private List<Task> taskList;
    private ArrayAdapter<Task> adapter;

    private Calendar calendar = Calendar.getInstance();
    private Task selectedTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        String userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) {
            Toast.makeText(this, "Ошибка: email не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = userEmail.replace(".", ",");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("reminders").child(currentUserId);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etDueDate = findViewById(R.id.etDueDate);
        btnAdd = findViewById(R.id.btnAdd);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnDelete = findViewById(R.id.btnDelete);
        btnDatePicker = findViewById(R.id.btnDatePicker);
        listView = findViewById(R.id.listView);

        taskList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, taskList);
        listView.setAdapter(adapter);

        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        btnAdd.setOnClickListener(v -> addTask());
        btnUpdate.setOnClickListener(v -> updateTask());
        btnDelete.setOnClickListener(v -> deleteTask());
        btnDatePicker.setOnClickListener(v -> showDatePicker());

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedTask = taskList.get(position);
                etTitle.setText(selectedTask.getTitle());
                etDescription.setText(selectedTask.getDescription());

                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                etDueDate.setText(sdf.format(new Date(selectedTask.getDueDate())));

                btnAdd.setEnabled(false);
                btnUpdate.setEnabled(true);
                btnDelete.setEnabled(true);
            }
        });

        // Проверяем наличие предзаполненных данных
        checkPrefilledData();

        loadTasks();
    }

    private void checkPrefilledData() {
        String prefilledTitle = getIntent().getStringExtra("prefilled_title");
        String prefilledDescription = getIntent().getStringExtra("prefilled_description");
        String prefilledDueDate = getIntent().getStringExtra("prefilled_due_date");

        if (prefilledTitle != null) {
            etTitle.setText(prefilledTitle);
        }
        if (prefilledDescription != null) {
            etDescription.setText(prefilledDescription);
        }
        if (prefilledDueDate != null) {
            etDueDate.setText(prefilledDueDate);

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                Date date = sdf.parse(prefilledDueDate);
                calendar.setTime(date);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                        etDueDate.setText(sdf.format(calendar.getTime()));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void addTask() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dueDateStr = etDueDate.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Введите название");
            return;
        }

        if (dueDateStr.isEmpty()) {
            etDueDate.setError("Выберите дату");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date dueDate = sdf.parse(dueDateStr);

            if (dueDate.before(new Date())) {
                etDueDate.setError("Дата должна быть в будущем");
                return;
            }

            String taskId = databaseReference.push().getKey();
            Task task = new Task(title, description, dueDate, currentUserId);
            task.setId(taskId);

            databaseReference.child(taskId).setValue(task)
                    .addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            clearFields();
                            Toast.makeText(RemindersActivity.this, "Напоминание добавлено", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RemindersActivity.this, "Ошибка добавления", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка формата даты", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTask() {
        if (selectedTask == null) return;

        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String dueDateStr = etDueDate.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Введите название");
            return;
        }

        if (dueDateStr.isEmpty()) {
            etDueDate.setError("Выберите дату");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            Date dueDate = sdf.parse(dueDateStr);

            if (dueDate.before(new Date())) {
                etDueDate.setError("Дата должна быть в будущем");
                return;
            }

            selectedTask.setTitle(title);
            selectedTask.setDescription(description);
            selectedTask.setDueDate(dueDate.getTime());

            databaseReference.child(selectedTask.getId()).setValue(selectedTask)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            clearFields();
                            Toast.makeText(RemindersActivity.this, "Напоминание обновлено", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(RemindersActivity.this, "Ошибка обновления", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка формата даты", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTask() {
        if (selectedTask == null) return;

        databaseReference.child(selectedTask.getId()).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        clearFields();
                        Toast.makeText(RemindersActivity.this, "Напоминание удалено", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RemindersActivity.this, "Ошибка удаления", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadTasks() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Task task = dataSnapshot.getValue(Task.class);
                    if (task != null) {
                        task.setId(dataSnapshot.getKey());
                        taskList.add(task);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RemindersActivity.this, "Ошибка загрузки напоминаний", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearFields() {
        etTitle.setText("");
        etDescription.setText("");
        etDueDate.setText("");
        selectedTask = null;
        btnAdd.setEnabled(true);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
    }
}