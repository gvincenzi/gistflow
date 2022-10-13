package com.gist.flow.ingester.impl;

import com.gist.flow.ingester.IFlowIngester;
import com.gist.flow.model.entity.FlowResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Set;

@Data
@Slf4j
@Component
public class TelegramBotIngester extends TelegramLongPollingBot implements IFlowIngester<FlowResource> {
    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.channelId}")
    private String channelId;

    private Integer maxMessage = 5;
    
    @Override
    public void onUpdateReceived(Update update) {
        logMessage(update);
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        for (Update update : updates) {
            logMessage(update);
        }
    }

    private void logMessage(Update update) {
        log.info(update.getMessage() != null ? update.getMessage().getFrom().getUserName() + " " + update.getMessage().getText() : "Message received");
    }

    private SendMessage message(String chatId, FlowResource content) {
        String text = "<b>" + content.getName() + "</b><br><br>" + content.getDescription();
        return new SendMessage()
                .setChatId(chatId)
                .enableHtml(true)
                .setText(text.replace("<br>", "\n"));
    }

    @Override
    public void ingest(Set<FlowResource> contents) {
        try {
            int i = 0;
            for (FlowResource content : contents) {
                i++;
                try {
                    execute(message("@"+channelId, content)); // Call method to send the message
                } catch (TelegramApiException e) {
                    log.error(e.getMessage());
                    continue;
                }

                if(i == maxMessage) return;

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    public void message(String text) {
        try {
            execute(new SendMessage()
                    .setChatId("@"+channelId)
                    .enableHtml(true)
                    .setText(text.replace("<br>", "\n"))
            ); // Call method to send the message
            Thread.sleep(5000);
        } catch (TelegramApiException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
