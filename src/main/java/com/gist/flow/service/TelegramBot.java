package com.gist.flow.service;

import com.gist.flow.sensor.impl.TelegramBotSensor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Data
@Slf4j
@Service
@Profile({"soffblog","gist"})
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    TelegramBotSensor telegramBotSensor;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public void onUpdateReceived(Update update) {
        telegramBotSensor.onUpdateReceived(update);
    }
}
