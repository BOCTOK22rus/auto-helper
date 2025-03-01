package com.example.autoHelperBot.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.example.autoHelperBot.specs.SQLiteHelper;

import static com.example.autoHelperBot.specs.Constants.*;
import static com.example.autoHelperBot.specs.Constants.USER_PASS;

@AllArgsConstructor
public class ApiStarline {
    private static final SQLiteHelper dbHelper = new SQLiteHelper();

    public static String getCookie() {
        String md5Secret = md5ApacheExample(APP_SECRET);
        String code = ApiStarline.getCode(APP_ID, md5Secret);
        String concatSecret = APP_SECRET + code;
        String tokenMd5Secret = md5ApacheExample(concatSecret);
        String token = ApiStarline.getToken(APP_ID, tokenMd5Secret);
        String slidToken = ApiStarline.userLogin(token, USER_EMAIL, USER_PASS);
        return ApiStarline.authSlid(slidToken);
    }

    @SneakyThrows
    public static String getParams(String deviceId, String cookie) {
        URL url = new URL("https://developer.starline.ru/json/v1/device/" + deviceId + "/obd_params");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String cookieHeader = "slnet=" + cookie;
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookieHeader);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
            }

            String responseBody = response.toString();
            JSONObject object = new JSONObject(responseBody);
            return Integer.toString(object.getJSONObject("obd_params").getJSONObject("mileage").getInt("val"));
        } else {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }
    }

    @SneakyThrows
    public static String getErrors(String deviceId, String cookie) {
        URL url = new URL("https://developer.starline.ru/json/v1/device/" + deviceId + "/obd_errors");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        String cookieHeader = "slnet=" + cookie;
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", cookieHeader);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
            }

            String responseBody = response.toString();
            JSONObject object = new JSONObject(responseBody);
            JSONArray obdErrors = object.getJSONArray("obd_errors");

            if (obdErrors.length() == 0) {
                return obdErrors.toString();
            }
            {
                StringBuilder errorsList = new StringBuilder();
                for (int i = 0; i < obdErrors.length(); i++) {
                    JSONObject errorObject = obdErrors.getJSONObject(i);
                    String errorCode = errorObject.getString("error");
                    String descriptionRu = errorObject.getJSONObject("descriptions").getString("ru");
                    long errorTs = errorObject.getLong("error_ts");
                    // Преобразование Unix Timestamp в Instant
                    Instant instant = Instant.ofEpochSecond(errorTs);
                    // Форматирование даты и времени
                    DateTimeFormatter formatter = DateTimeFormatter
                            .ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.of("Asia/Barnaul"));
                    String formattedDate = formatter.format(instant);
                    String currentTime = getCurrentDateTime();
                    errorsList
                            .append("Ошибка: ").append(descriptionRu).append("\n")
                            .append("Код: ").append(errorCode).append("\n")
                            .append("Время: ").append(formattedDate).append("\n").append("\n");
                    dbHelper.insertErrors(errorCode, descriptionRu, formattedDate, currentTime);
                }
                return errorsList.toString();
            }
        } else {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }
    }

    @SneakyThrows
    public static String getCode(String appId, String appSecret) {
        URL url = new URL("https://id.starline.ru/apiV3/application/getCode" + "?appId=" + appId + "&secret=" + appSecret);
        Scanner scanner = new Scanner((InputStream) url.getContent());
        StringBuilder result = new StringBuilder();
        while (scanner.hasNext()) {
            result.append(scanner.nextLine());
        }
        JSONObject object = new JSONObject(result.toString());
        return object.getJSONObject("desc").getString("code");
    }

    @SneakyThrows
    public static String getToken(String appId, String appCode) {
        URL url = new URL("https://id.starline.ru/apiV3/application/getToken" + "?appId=" + appId + "&secret=" + appCode);
        Scanner scanner = new Scanner((InputStream) url.getContent());
        StringBuilder result = new StringBuilder();
        while (scanner.hasNext()) {
            result.append(scanner.nextLine());
        }
        JSONObject object = new JSONObject(result.toString());
        return object.getJSONObject("desc").getString("token");
    }

    @SneakyThrows
    public static String userLogin(String token, String email, String pass) {

        URL url = new URL("https://id.starline.ru/apiV3/user/login");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("token", token);
        connection.setRequestProperty("user_ip", "100.112.68.208");
        connection.setDoOutput(true); // Разрешаем отправку тела запроса

        // Создаем JSON-объект для тела запроса
        String requestBody = "login=" + email + "&pass=" + pass;

        // Отправляем тело запроса
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Получаем ответ
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(connection.getInputStream())) {
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
            }

            String responseBody = response.toString();
            JSONObject object = new JSONObject(responseBody);

            return object.getJSONObject("desc").getString("user_token");
        } else {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }
    }

    @SneakyThrows
    public static String authSlid(String slidToken) {

        URL url = new URL("https://developer.starline.ru/json/v2/auth.slid");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true); // Разрешаем отправку тела запроса

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("slid_token", slidToken);

        // Отправляем тело запроса
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Получаем ответ
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Получаем cookies из заголовков ответа
            Map<String, List<String>> headerFields = connection.getHeaderFields();
            List<String> cookiesHeader = headerFields.get("Set-Cookie");

            String specificCookieValue = null;
            String cookieKeyToFind = "slnet";

            if (cookiesHeader != null) {
                for (String cookie : cookiesHeader) {
                    if (cookie.startsWith(cookieKeyToFind + "=")) {
                        // Разделяем cookie по символу ';' и берем первую часть
                        specificCookieValue = cookie.split(";")[0].split("=")[1];
                        break; // Выходим из цикла, если нашли нужный cookie
                    }
                }
            }

            return specificCookieValue;
        } else {
            throw new RuntimeException("Failed : HTTP error code : " + responseCode);
        }
    }

    private static String md5ApacheExample(String text) {
        return DigestUtils.md5Hex(text);
    }

    private static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return now.format(formatter);
    }
}
