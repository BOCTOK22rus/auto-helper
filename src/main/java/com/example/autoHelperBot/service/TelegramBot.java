package com.example.autoHelperBot.service;

import com.example.autoHelperBot.config.BotConfig;
import com.example.autoHelperBot.specs.SQLiteHelper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.expression.ParseException;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static com.example.autoHelperBot.specs.Constants.*;

@Component
@AllArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private BotConfig botConfig;

    private SQLiteHelper dbHelper;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        String userMessage;

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getUserName();

            if (userName == null) {
                userName = "Группа " + update.getMessage().getChat().getTitle();
            } else {
                userName = "@" + userName;
            }

            // Проверяем команды и обрабатываем их
            if (messageText.startsWith("/")) {
                // Извлекаем команду
                String command = messageText.split(" ")[0];
                // Извлекаем текст после команды
                String commandArgument = messageText.length() > command.length() ? messageText.substring(command.length()).trim() : "";

                switch (command) {
                    case "/start" -> {
                        recordUsersRequests(userName, "start");
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    }
                    case "/mileage" -> {
                        recordUsersRequests(userName, "mileage");
                        mileageCommandReceived(chatId);
                    }
                    case "/engine", "/trans" -> {
                        recordUsersRequests(userName, command.equals("/engine") ? "engine" : "trans");
                        // Проверяем, является ли аргумент целым числом
                        if (isInteger(commandArgument)) {
                            int mileageValue = Integer.parseInt(commandArgument);
                            if (command.equals("/engine")) {
                                addMilEnginCommandReceived(chatId, mileageValue);
                            } else {
                                addMilTransCommandReceived(chatId, mileageValue);
                            }
                        } else {
                            sendMessage(chatId, "Ошибка: аргумент должен быть целым числом.");
                        }
                    }
                    case "/errors" -> {
                        recordUsersRequests(userName, "errors");
                        errorsCommandReceived(chatId);
                    }
                    case "/requests" -> {
                        recordUsersRequests(userName, "requests");
                        returnUsersRequests(chatId);
                    }
                    default -> {
                        try {
                            recordUsersRequests(userName, "сообщение: " + "\"" + messageText + "\"");
                            userMessage = userResponse(messageText);
                        } catch (ParseException e) {
                            throw new RuntimeException("Что-то пошло не так");
                        }
                        sendMessage(chatId, userMessage);
                    }
                }
            } else {
                // Обработка обычного сообщения
                try {
                    recordUsersRequests(userName, "сообщение: " + "\"" + messageText + "\"");
                    userMessage = userResponse(messageText);
                } catch (ParseException e) {
                    throw new RuntimeException("Что-то пошло не так");
                }
                sendMessage(chatId, userMessage);
            }
        }
    }

    private void recordUsersRequests(String name, String command) {
        String timeStamp = getCurrentDateTime();
        dbHelper.insertRequests(name, command, timeStamp);
    }

    private void returnUsersRequests(Long chatId) {
        String answer = dbHelper.selectAllRequests();
        sendMessage(chatId, answer);
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Привет " + name + "!" + "\n" + "\n"
                + "Доступные команды:" + "\n"
                + "/start - доступные команды" + "\n" + "\n"
                + "/mileage - получить текущий пробег" + "\n" + "\n"
                + "/engine - зафиксировать текущий пробег и установить километраж до следующей замены масла в двигателе" + "\n" + "\n"
                + "/trans - зафиксировать текущий пробег и установить километраж до следующей замены масла в АКПП" + "\n" + "\n"
                + "/errors - получить ошибки" + "\n" + "\n"
                + "/requests - получить 5 последних запросов" + "\n" + "\n";
        sendMessage(chatId, answer);
    }

    private String userResponse(String message) {
        return "Я не знаю что такое " + "\"" + message + "\"" + " отправь команду из списка.";
    }

    private void mileageCommandReceived(Long chatId) {
        String cookie = ApiStarline.getCookie();
        String answer = ApiStarline.getParams(DEVICE_ID, cookie);
        answer = "текущий пробег " + answer + " км";
        sendMessage(chatId, answer);
    }

    private void addMilEnginCommandReceived(Long chatId, int mileageReplacement) {
        String cookie = ApiStarline.getCookie();
        String mileage = ApiStarline.getParams(DEVICE_ID, cookie);
        String timeStamp = getCurrentDateTime();
        int nextReplyOilMileage = Integer.parseInt(mileage.trim()) + mileageReplacement;
        dbHelper.insertEngine(mileage, Integer.toString(nextReplyOilMileage), timeStamp);

        String answer = "Значение " + mileageReplacement +
                " км до следующей замены масла в двигателе установлено. Замена при достижении " +
                nextReplyOilMileage + " км.";
        sendMessage(chatId, answer);
    }

    private void addMilTransCommandReceived(Long chatId, int mileageReplacement) {
        String cookie = ApiStarline.getCookie();
        String mileage = ApiStarline.getParams(DEVICE_ID, cookie);
        String timeStamp = getCurrentDateTime();
        int nextReplyOilMileage = Integer.parseInt(mileage.trim()) + mileageReplacement;
        dbHelper.insertTransmission(mileage, Integer.toString(nextReplyOilMileage), timeStamp);

        String answer = "Значение " + mileageReplacement +
                " км до следующей замены масла в АКПП установлено. Замена при достижении " +
                nextReplyOilMileage + " км.";
        sendMessage(chatId, answer);
    }

    private void errorsCommandReceived(Long chatId) {
        String cookie = ApiStarline.getCookie();
        String answer = ApiStarline.getErrors(DEVICE_ID, cookie);
        if (Objects.equals(answer, "[]")) {
            answer = "Ошибок нет";
        } else {
            answer = "Список ошибок: " + answer;
        }
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException ignored) {
        }
    }

    private String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return now.format(formatter);
    }

    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}