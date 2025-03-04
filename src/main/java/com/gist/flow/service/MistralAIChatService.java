package com.gist.flow.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
@Profile({"soffblog","gist"})
public class MistralAIChatService {
	@Autowired
	MistralAiChatModel chatModel;
	
	@Value("classpath:/prompts/job-role-informations.st")
	private Resource jobRoleInformationsResource;
	
	@Value("classpath:/prompts/top-10.st")
	private Resource top10Resource;
	
	@Value("classpath:/prompts/job-role-interview.st")
	private Resource jobRoleInterviewResource;
	
	@Value("classpath:/prompts/translation.st")
	private Resource translationResource;
	
	@Value("${telegram.bot.assistant.title}")
	private String assistantTitle;
	
	public String getJobRoleInformations(String message) {
		UserMessage userMessage = new UserMessage(message);
		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.jobRoleInformationsResource);
		Message systemMessage = systemPromptTemplate.createMessage(Map.of("name",assistantTitle, "state", "Italy", "language", "italian"));
		Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
		ChatResponse response = this.chatModel.call(prompt);
		return response.getResult().getOutput().getText();
	}

	public String getTop10() {
		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.top10Resource);
		Message systemMessage = systemPromptTemplate.createMessage(Map.of("name",assistantTitle, "state", "Italy", "language", "italian"));
		Prompt prompt = new Prompt(List.of(systemMessage));
		ChatResponse response = this.chatModel.call(prompt);
		return response.getResult().getOutput().getText();
	}
	
	public String getJobRoleInterview(String message) {
		UserMessage userMessage = new UserMessage(message);
		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.jobRoleInterviewResource);
		Message systemMessage = systemPromptTemplate.createMessage(Map.of("name",assistantTitle, "state", "Italy", "language", "italian"));
		Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
		ChatResponse response = this.chatModel.call(prompt);
		return response.getResult().getOutput().getText();
	}
	
	public String translateQuote(String message, String language) {
		UserMessage userMessage = new UserMessage(message);
		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(this.translationResource);
		Message systemMessage = systemPromptTemplate.createMessage(Map.of("language", "italian"));
		Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
		ChatResponse response = this.chatModel.call(prompt);
		return response.getResult().getOutput().getText();
	}
}
