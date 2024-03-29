package com.gist.flow.ingester.impl;

import com.gist.flow.ingester.IFlowIngester;
import com.gist.flow.model.entity.FlowResource;
import com.gist.flow.service.TelegramBot;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Set;

@Data
@Slf4j
@Profile({"soffblog","gist"})
@Component
public class TelegramBotIngester implements IFlowIngester<FlowResource> {
    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.channelId}")
    private String channelId;

    private Integer maxMessage = 5;

    @Autowired
    private TelegramBot telegramBot;
    
    private SendMessage message(String chatId, FlowResource content) {
        String text = "<b>" + content.getName() + "</b><br><br>" + content.getDescription();
        return prepareMessage(chatId,text
                .replace("<br>", "\n")
                .replace("<BR>", "\n")
                .replace("<p>", "\n")
                .replace("<P>", "\n")
                .replace("</p>", "\n")
                .replace("</P>", "\n"));
    }

    @Override
    public void ingest(Set<FlowResource> contents) {
        try {
            int i = 0;
            for (FlowResource content : contents) {
                i++;
                try {
                    telegramBot.execute(message("@"+channelId, content)); // Call method to send the message
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

    public SendMessage prepareMessage(String chat_id, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat_id);
        sendMessage.setText(text);
        sendMessage.enableHtml(Boolean.TRUE);
        return sendMessage;
    }

    public void message(String text) {
        try {
            telegramBot.execute(prepareMessage("@"+channelId,text.replace("<br>", "\n")));
            Thread.sleep(5000);
        } catch (TelegramApiException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
