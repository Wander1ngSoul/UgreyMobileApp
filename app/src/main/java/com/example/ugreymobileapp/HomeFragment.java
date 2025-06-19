package com.example.ugreymobileapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String BEARER_TOKEN = "Bearer 8DWQLfproEJlyC8dJaLqRhBx1B2sJyZR4V";
    private static final String BASE_URL = "http://80.93.179.130:9099/";
    private static final int MAX_RETRIES = 80;
    private static final long RETRY_DELAY_MS = 5000;
    private static final int REQUEST_TIMEOUT_MS = 10000;

    private String userEmail;
    private ImageView imagePreview;
    private Button uploadButton;
    private Button addTaskButton;
    private ProgressBar progressBar;
    private TextView resultText;
    private TextView statusText;
    private Bitmap selectedBitmap;
    private RequestQueue requestQueue;
    private int retryCount = 0;
    private Handler handler = new Handler();
    private String currentTaskId;
    private String lastMeterReading;
    private String lastSerialNumber;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userEmail = getArguments().getString("email");
        }

        if (userEmail == null && getActivity() != null) {
            userEmail = getActivity().getIntent().getStringExtra("email");
        }
        requestQueue = Volley.newRequestQueue(requireContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);



        if (getArguments() != null) {
            userEmail = getArguments().getString("email");
            String userName = getArguments().getString("userName");

            TextView welcomeText = view.findViewById(R.id.welcome_text);
            if (welcomeText != null) {
                if (userName != null && !userName.isEmpty()) {
                    welcomeText.setText("Добро пожаловать, " + userName + "!");
                } else {
                    welcomeText.setText("Добро пожаловать!");
                }
            }
        }

        addTaskButton = view.findViewById(R.id.add_task_button);
        imagePreview = view.findViewById(R.id.image_preview);
        uploadButton = view.findViewById(R.id.upload_button);
        progressBar = view.findViewById(R.id.progress_bar);
        resultText = view.findViewById(R.id.result_text);
        statusText = view.findViewById(R.id.status_text);

        uploadButton.setOnClickListener(v -> openImageChooser());
        addTaskButton.setOnClickListener(v -> openReminders());

        return view;
    }

    private void redirectToAuth() {
        startActivity(new Intent(getActivity(), AuthActivity.class));
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(
                        requireActivity().getContentResolver(), imageUri);
                imagePreview.setImageBitmap(selectedBitmap);
                imagePreview.setVisibility(View.VISIBLE);
                uploadImageToServer();
            } catch (IOException e) {
                e.printStackTrace();
                showToast("Ошибка загрузки изображения");
            }
        }
    }

    private void uploadImageToServer() {
        if (selectedBitmap == null) {
            showToast("Изображение не выбрано");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        resultText.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Отправка изображения...");
        retryCount = 0;
        currentTaskId = null;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        String url = BASE_URL + "tasks";

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                Request.Method.POST, url,
                response -> {
                    try {
                        String responseString = new String(response.data, StandardCharsets.UTF_8);
                        Log.d("API_RESPONSE", "Response: " + responseString);
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (!jsonResponse.has("task_id")) {
                            throw new JSONException("Ответ сервера не содержит task_id");
                        }

                        currentTaskId = jsonResponse.getString("task_id");
                        String initialStatus = jsonResponse.optString("status", "waiting");
                        statusText.setText("Статус: " + initialStatus);
                        checkTaskStatus(currentTaskId);
                    } catch (JSONException e) {
                        progressBar.setVisibility(View.GONE);
                        statusText.setVisibility(View.GONE);
                        showToast("Ошибка формата ответа: " + e.getMessage());
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setVisibility(View.GONE);

                    if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                        handleAuthError();
                        return;
                    }

                    String errorMsg = "Ошибка загрузки";
                    if (error.networkResponse != null) {
                        errorMsg += " (код: " + error.networkResponse.statusCode + ")";
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            errorMsg += ": " + responseBody;
                            Log.e("UPLOAD_ERROR", responseBody);
                        } catch (Exception e) {
                            Log.e("UPLOAD_ERROR", "Ошибка чтения тела ошибки");
                        }
                    }
                    showToast(errorMsg);
                });

        multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        multipartRequest.addByteData("image",
                new VolleyMultipartRequest.DataPart("image.jpg", imageBytes, "image/jpeg"));

        requestQueue.add(multipartRequest);
    }

    private void checkTaskStatus(String taskId) {
        if (retryCount >= MAX_RETRIES) {
            handler.removeCallbacksAndMessages(null);
            progressBar.setVisibility(View.GONE);
            statusText.setText("Превышено время ожидания");
            showToast("Превышено количество попыток проверки статуса");
            return;
        }

        String statusUrl = BASE_URL + "status?uuid=" + taskId;

        JsonObjectRequest statusRequest = new JsonObjectRequest(
                Request.Method.GET, statusUrl, null,
                response -> {
                    try {
                        Log.d("STATUS_RESPONSE", response.toString());

                        if (!response.has("status")) {
                            throw new JSONException("Отсутствует поле status в ответе");
                        }

                        String status = response.getString("status");
                        statusText.setText("Статус: " + status);

                        switch (status) {
                            case "Completed":
                                getTaskResult(taskId);
                                break;
                            case "Failed":
                                progressBar.setVisibility(View.GONE);
                                statusText.setText("Ошибка обработки");
                                showToast("Сервер сообщил об ошибке обработки");
                                break;
                            case "In Process":
                                retryCount++;
                                handler.postDelayed(() -> checkTaskStatus(taskId), RETRY_DELAY_MS * 2);
                                break;
                            default:
                                retryCount++;
                                handler.postDelayed(() -> checkTaskStatus(taskId), RETRY_DELAY_MS);
                                break;
                        }
                    } catch (JSONException e) {
                        progressBar.setVisibility(View.GONE);
                        statusText.setVisibility(View.GONE);
                        showToast("Ошибка обработки статуса: " + e.getMessage());
                    }
                },
                error -> {
                    retryCount++;
                    if (retryCount >= MAX_RETRIES) {
                        progressBar.setVisibility(View.GONE);
                        statusText.setText("Ошибка соединения");
                        showToast("Ошибка проверки статуса: " + error.getMessage());
                    } else {
                        handler.postDelayed(() -> checkTaskStatus(taskId), RETRY_DELAY_MS);
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", BEARER_TOKEN);
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        statusRequest.setRetryPolicy(new DefaultRetryPolicy(
                REQUEST_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(statusRequest);
    }

    private void getTaskResult(String taskId) {
        String resultUrl = BASE_URL + "result?uuid=" + taskId;
        Log.d("API_REQUEST", "Fetching result from: " + resultUrl);

        JsonObjectRequest resultRequest = new JsonObjectRequest(
                Request.Method.GET, resultUrl, null,
                response -> {
                    try {
                        Log.d("API_RESPONSE", "Raw response: " + response.toString());

                        if (!response.has("result")) {
                            Log.e("API_ERROR", "Response missing 'result' field");
                            throw new JSONException("Отсутствует обязательное поле 'result' в ответе");
                        }

                        JSONObject result = response.getJSONObject("result");
                        Log.d("API_DATA", "Result object: " + result.toString());

                        String meterReading = result.optString("meter_reading", null);
                        String serialNumber = result.optString("serial_number", null);

                        if (meterReading == null || serialNumber == null) {
                            Log.e("API_ERROR", "Missing required fields in result");
                            throw new JSONException("Неполные данные в результате");
                        }

                        lastMeterReading = meterReading;
                        lastSerialNumber = serialNumber;

                        String resultString = String.format(
                                "Показания счетчика: %s\nСерийный номер: %s",
                                meterReading,
                                serialNumber
                        );

                        requireActivity().runOnUiThread(() -> {
                            resultText.setText(resultString);
                            resultText.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            statusText.setVisibility(View.GONE);

                            showReminderDialog();
                        });

                    } catch (JSONException e) {
                        Log.e("API_ERROR", "JSON parsing error", e);
                        requireActivity().runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            statusText.setVisibility(View.GONE);
                            showToast("Ошибка формата данных: " + e.getMessage());
                        });
                    }
                },
                error -> {
                    Log.e("API_ERROR", "Result fetch error", error);
                    String errorDetails = "Ошибка получения данных";

                    if (error.networkResponse != null) {
                        errorDetails += " (HTTP " + error.networkResponse.statusCode + ")";
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            errorDetails += ": " + responseBody;
                            Log.e("API_ERROR_BODY", responseBody);
                        } catch (Exception e) {
                            Log.e("API_ERROR", "Error reading error response", e);
                        }
                    } else if (error.getMessage() != null) {
                        errorDetails += ": " + error.getMessage();
                    }

                    final String finalErrorDetails = errorDetails;
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        statusText.setVisibility(View.GONE);
                        showToast(finalErrorDetails);

                        if (currentTaskId != null && retryCount < MAX_RETRIES / 2) {
                            retryCount++;
                            handler.postDelayed(() -> getTaskResult(currentTaskId), RETRY_DELAY_MS);
                        }
                    });
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", BEARER_TOKEN);
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                return headers;
            }

            @Override
            public Priority getPriority() {
                return Priority.HIGH;
            }
        };

        resultRequest.setRetryPolicy(new DefaultRetryPolicy(
                REQUEST_TIMEOUT_MS * 2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(resultRequest);
    }

    private void showReminderDialog() {
        if (lastMeterReading == null || lastSerialNumber == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Создать напоминание?");
        builder.setMessage("Хотите создать напоминание для этих показаний счетчика?");

        builder.setPositiveButton("Да", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                createReminderAutomatically();
            }
        });

        builder.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void createReminderAutomatically() {
        if (lastMeterReading == null || lastSerialNumber == null || userEmail == null) return;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 7);
        Date dueDate = calendar.getTime();

        String title = "Показания счетчика " + lastSerialNumber;
        String description = "Текущие показания: " + lastMeterReading;

        // Создаем Intent для открытия RemindersActivity с предзаполненными данными
        Intent intent = new Intent(requireActivity(), RemindersActivity.class);
        intent.putExtra("email", userEmail);
        intent.putExtra("prefilled_title", title);
        intent.putExtra("prefilled_description", description);

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        intent.putExtra("prefilled_due_date", sdf.format(dueDate));

        startActivity(intent);
    }

    private void handleAuthError() {
        showToast("Ошибка авторизации: неверный токен");
        if (getActivity() != null) {
            startActivity(new Intent(getActivity(), AuthActivity.class));
            getActivity().finish();
        }
    }

    private void showToast(String message) {
        try {
            if (getContext() != null && getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
            }
        } catch (Exception e) {
            Log.e("UI_ERROR", "Toast error", e);
        }
    }

    private void openReminders() {
        if (userEmail != null) {
            Intent intent = new Intent(requireActivity(), RemindersActivity.class);
            intent.putExtra("email", userEmail);
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Ошибка: email не найден", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        if (requestQueue != null) {
            requestQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
        }
    }
}