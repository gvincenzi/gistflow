package com.gist.flow.ingester.impl;

import lombok.Data;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Data
@Log
@Component
public class TelegramBotIngester extends TelegramLongPollingBot {
    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public void onUpdateReceived(Update update) {
        logMessage(update);
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        for (Update update:updates) {
            logMessage(update);
        }
    }

    private void logMessage(Update update) {
        log.info(update.getMessage() != null ? update.getMessage().getAuthorSignature() + " " + update.getMessage().getText() : "Message received");
    }
}
