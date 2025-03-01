package com.example.autoHelperBot.service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.example.autoHelperBot.specs.SQLiteHelper;
import lombok.SneakyThrows;
import org.json.JSONObject;

import static com.example.autoHelperBot.specs.Constants.DEVICE_ID;

public abstract class Scheduler {
    private final ScheduledExecutorService scheduler;
    private static final SQLiteHelper dbHelper = new SQLiteHelper();

    public Scheduler() {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void scheduleApiCallAtFixedRate(long initialDelay, long period, TimeUnit timeUnit) {
        scheduler.scheduleAtFixedRate(this::getMileageReplace, initialDelay, period, timeUnit);
    }

    public void getMileageReplace() {
        int currentMileage = Integer.parseInt(getMileage());
        int mileageEngineReplace = Integer.parseInt(dbHelper.selectMileage(
                "SELECT * FROM engine ORDER BY id DESC LIMIT 1"));
        int mileageTransmissionReplace = Integer.parseInt(dbHelper.selectMileage(
                "SELECT * FROM transmission ORDER BY id DESC LIMIT 1"));

        if (mileageEngineReplace <= currentMileage) {
            sendNotification("-4631024953", "Пора менять масло в двигателе");
        }
        if (mileageTransmissionReplace <= currentMileage) {
            sendNotification("-4631024953", "Пора менять масло в коробке");
        }
    }

    public String getMileage() {
        String cookie = ApiStarline.getCookie();
        return ApiStarline.getParams(DEVICE_ID, cookie);
    }

    @SneakyThrows
    public void sendNotification(String chatId, String message) {

        URL url = new URL("https://api.telegram.org/bot7599824780:AAH0dk2R9GYwS4axzIPrfWE_P9UQvy3_38Y/sendMessage");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("chat_id", chatId);
        jsonBody.put("text", message);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }
    }
}