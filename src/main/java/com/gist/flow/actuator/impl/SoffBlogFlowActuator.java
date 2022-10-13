package com.gist.flow.actuator.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.exception.FlowException;
import com.gist.flow.ingester.impl.TelegramBotIngester;
import com.gist.flow.ingester.impl.WordPressIngester;
import com.gist.flow.model.entity.FlowResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Set;

@Slf4j
@Service
@Profile("soffblog")
public class SoffBlogFlowActuator implements IFlowActuator<FlowResource> {
	@Autowired
	private WordPressIngester wordPressIngester;

	@Autowired
	private TelegramBotIngester telegramBotIngester;

	@Value("${telegram.bot.messageHTML}")
	private String messageHTML;

	@Override
	public void doAction(Set<FlowResource> resources) throws FlowException {
		log.info(String.format("SoffBlogFlowActuator doAction has been called with [%d] resources",resources.size()));

		wordPressIngester.ingest(resources);

		telegramBotIngester.message(String.format(messageHTML,resources.size()));
		telegramBotIngester.ingest(resources);

		log.info(String.format("SoffBlogFlowActuator doAction has been successfully with [%d] resources",resources.size()));
	}
	
	public Calendar getLastPostDate() throws FlowException{
		return wordPressIngester.getLastPostDate();
	}

}
