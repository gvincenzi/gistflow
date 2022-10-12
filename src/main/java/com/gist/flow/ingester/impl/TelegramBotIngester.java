package com.gist.flow.ingester.impl;

import com.gist.flow.ingester.IFlowIngester;
import com.gist.flow.model.entity.FlowResource;
import lombok.Data;
import lombok.extern.java.Log;
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

    private SendMessage message(String chat_id, FlowResource content) {
        String text = "<b>" + content.getName() + "</b><br><br>" + content.getDescription();
        return new SendMessage()
                .setChatId(chat_id)
                .enableHtml(true)
                .setText(text.replaceAll("<br>", "\n"));
    }

    private SendMessage message(String chat_id, String text) {
        return new SendMessage()
                .setChatId(chat_id)
                .enableHtml(true)
                .setText(text.replaceAll("<br>", "\n"));
    }

    @Override
    public void ingest(Set<FlowResource> contents) {
        try {
            execute(message("@gist_it", String.format("<i>Soffblog</i> ha trovato <b>%d</b> nuova(e) offerta(e) di lavoro.<br>Puoi consultarle sul <a href='https://soffblog.altervista.org'>sito</a> o direttamente qui dal canale.",contents.size())));
            Thread.sleep(5000);
            for (FlowResource content : contents) {
                execute(message("@gist_it", content)); // Call method to send the message
                Thread.sleep(1000);
            }
        } catch (TelegramApiException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    /*public void sendMessage(String text) {
        try {
            FlowResource flowResource = new FlowResource();
            flowResource.setName("JAVA DEVELOPER");
            flowResource.setDescription("Michael Page<br><a href='http://lavoro.corriere.it/Annunci/Java_Developer_800894166.htm'>(\n" +
                    "\t\t\t\t\t\t\thttp://lavoro.corriere...)</a>");
            execute(message("@gist_it", flowResource)); // Call method to send the message
        } catch (TelegramApiException e) {
            log.severe(e.getMessage());
        }
    }*/
}
