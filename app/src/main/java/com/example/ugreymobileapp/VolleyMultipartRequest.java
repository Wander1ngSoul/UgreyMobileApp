package com.example.ugreymobileapp;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.DefaultRetryPolicy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class VolleyMultipartRequest extends Request<NetworkResponse> {
    private final Response.Listener<NetworkResponse> mListener;
    private final Response.ErrorListener mErrorListener;
    private final Map<String, DataPart> mByteData;
    private final String BOUNDARY = "Volley-" + System.currentTimeMillis();
    private final String LINE_FEED = "\r\n";
    private static final int TIMEOUT_MS = 10000;

    public VolleyMultipartRequest(int method, String url,
                                  Response.Listener<NetworkResponse> listener,
                                  Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
        mErrorListener = errorListener;
        mByteData = new HashMap<>();
        setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    public void addByteData(String key, DataPart data) {
        mByteData.put(key, data);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer 8DWQLfproEJlyC8dJaLqRhBx1B2sJyZR4V");
        headers.put("Accept", "application/json");
        return headers;
    }

    @Override
    public String getBodyContentType() {
        return "multipart/form-data;boundary=" + BOUNDARY;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            for (Map.Entry<String, DataPart> entry : mByteData.entrySet()) {
                buildDataPart(bos, entry.getValue(), entry.getKey());
            }
            bos.write(("--" + BOUNDARY + "--" + LINE_FEED).getBytes());
            return bos.toByteArray();
        } catch (IOException e) {
            VolleyLog.e("IOException writing to ByteArrayOutputStream");
            return null;
        }
    }

    private void buildDataPart(ByteArrayOutputStream bos, DataPart dataPart, String fieldName) throws IOException {
        bos.write(("--" + BOUNDARY + LINE_FEED).getBytes());
        bos.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" +
                dataPart.getFileName() + "\"" + LINE_FEED).getBytes());
        bos.write(("Content-Type: " + dataPart.getType() + LINE_FEED).getBytes());
        bos.write(LINE_FEED.getBytes());
        bos.write(dataPart.getContent());
        bos.write(LINE_FEED.getBytes());
    }

    @Override
    protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
        return Response.success(response, getCacheEntry());
    }

    @Override
    protected void deliverResponse(NetworkResponse response) {
        mListener.onResponse(response);
    }

    @Override
    public void deliverError(VolleyError error) {
        if (error instanceof AuthFailureError) {
            error = new VolleyError("Ошибка авторизации: проверьте токен");
        } else if (error.networkResponse == null && error.getCause() instanceof java.net.SocketTimeoutException) {
            error = new VolleyError("Таймаут соединения. Сервер не отвечает");
        } else if (error.networkResponse != null && error.networkResponse.statusCode >= 500) {
            error = new VolleyError("Ошибка сервера. Попробуйте позже");
        }
        mErrorListener.onErrorResponse(error);
    }

    public static class DataPart {
        private String fileName;
        private byte[] content;
        private String type;

        public DataPart(String name, byte[] data, String mimeType) {
            fileName = name;
            content = data;
            type = mimeType;
        }

        public String getFileName() {
            return fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getType() {
            return type;
        }
    }
}