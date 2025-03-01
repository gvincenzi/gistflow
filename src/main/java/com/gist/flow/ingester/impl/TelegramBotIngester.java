package com.gist.flow.ingester.impl;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.gist.flow.ingester.IFlowIngester;
import com.gist.flow.model.entity.FlowResource;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Profile({"soffblog","gist"})
@Component
public class TelegramBotIngester implements IFlowIngester<FlowResource> {
	@Value("${telegram.bot.username}")
    private String botUsername;
	
	@Value("${telegram.bot.channelId}")
    private String channelId;

    @Value("${telegram.bot.token}")
    private String botToken;

    private Integer maxMessage = 5;

    private final TelegramClient telegramClient;

    public TelegramBotIngester(@Value("${telegram.bot.token}") String botToken) {
        telegramClient = new OkHttpTelegramClient(botToken);
    }
    
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
                	telegramClient.execute(message(content.getRecipientID()!=null?content.getRecipientID():("@"+channelId), content)); // Call method to send the message
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
        SendMessage sendMessage = new SendMessage(chat_id,text);
        sendMessage.enableHtml(Boolean.TRUE);
        return sendMessage;
    }

    public void message(String text, String channelId) {
        try {
        	telegramClient.execute(prepareMessage(channelId,text.replace("<br>", "\n")));
            Thread.sleep(5000);
        } catch (TelegramApiException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }
}
