package com.gist.flow.sensor.impl;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.actuator.impl.GistFlowActuator;
import com.gist.flow.actuator.impl.SoffBlogFlowActuator;
import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import com.gist.flow.sensor.IFlowSensor;
import com.gist.flow.service.MistralAIChatService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
@Profile({"soffblog","gist"})
public class TelegramAIAssistantBotSensor implements IFlowSensor<FlowResource> {
	private static final String START = "/start";
	private static final String JOB_ROLE = "/job";
    private static final String TOP_10 = "/top10";
    private static final String JOB_INTERVIEW = "/int";
    
    @Value("${telegram.bot.username}")
    private String botUsername;
    
    @Value("${telegram.bot.channelId}")
    private String channelId;

    @Value("${telegram.bot.admin.id}")
    private Long adminUserId;

    @Value("${telegram.bot.admin.title}")
    private String adminTitle;
    
    @Value("${telegram.bot.assistant.title}")
    private String assistantTitle;
    
    @Value("${telegram.bot.assistant.startMessageHTML}")
    private String startMessageHTML;

    private Set<IFlowActuator<FlowResource>> flowActuators = new HashSet<>();

    private HashSet<FlowResource> flowResources = new HashSet<>(1);
    
    @Autowired
    private MistralAIChatService mistralAIChatService;

    public void onUpdateReceived(Update update) {
        flowResources.clear();
        processUpdate(flowResources, update);
        try {
            this.onChange(flowResources);
        } catch (FlowException e) {
            log.error(e.getMessage());
        }
    }

    private void processUpdate(HashSet<FlowResource> flowResources, Update update) {
        logMessage(update);
        if (update.hasMessage() && update.getMessage().getText().startsWith(START)) {
            Long user_id = update.getMessage().getFrom().getId();
            if(adminUserId.equals(user_id)){
                FlowResource flowResource = new FlowResource();
                flowResource.setStartDateOfValidity(Calendar.getInstance());
                flowResource.setName(assistantTitle);
                flowResource.setDescription(startMessageHTML);
                flowResource.setRecipientID(Long.toString(update.getMessage().getChatId()));
                
                flowResources.add(flowResource);
            }
        } else if (update.hasMessage() && update.getMessage().getText().startsWith(JOB_ROLE)) {
        	if(update.getMessage().getText().length() == JOB_ROLE.length()) {
        		FlowResource flowResource = new FlowResource();
                flowResource.setStartDateOfValidity(Calendar.getInstance());
                flowResource.setName(assistantTitle);
                flowResource.setDescription("Bisogna scrivere il ruolo dopo '/job'");
                flowResource.setRecipientID(Long.toString(update.getMessage().getChatId()));

                flowResources.add(flowResource);
        	} else {
	            Long user_id = update.getMessage().getFrom().getId();
	            if(adminUserId.equals(user_id)){
	                FlowResource flowResource = new FlowResource();
	                flowResource.setStartDateOfValidity(Calendar.getInstance());
	                flowResource.setName(assistantTitle);
	                flowResource.setDescription(mistralAIChatService.getJobRoleInformations(update.getMessage().getText().substring(JOB_ROLE.length())));
	                flowResource.setRecipientID(Long.toString(update.getMessage().getChatId()));
	
	                flowResources.add(flowResource);
	            }
        	}
        } else if (update.hasMessage() && update.getMessage().getText().startsWith(TOP_10)) {
        	Long user_id = update.getMessage().getFrom().getId();
            if(adminUserId.equals(user_id)){
                FlowResource flowResource = new FlowResource();
                flowResource.setStartDateOfValidity(Calendar.getInstance());
                flowResource.setName(assistantTitle);
                flowResource.setDescription(mistralAIChatService.getTop10());
                flowResource.setRecipientID(Long.toString(update.getMessage().getChatId()));

                flowResources.add(flowResource);
            } 
        } else if (update.hasMessage() && update.getMessage().getText().startsWith(JOB_INTERVIEW)) {
        	Long user_id = update.getMessage().getFrom().getId();
            if(adminUserId.equals(user_id)){
                FlowResource flowResource = new FlowResource();
                flowResource.setStartDateOfValidity(Calendar.getInstance());
                flowResource.setName(assistantTitle);
                flowResource.setDescription(mistralAIChatService.getJobRoleInterview(update.getMessage().getText().substring(JOB_INTERVIEW.length())));
                flowResource.setRecipientID(Long.toString(update.getMessage().getChatId()));

                flowResources.add(flowResource);
            } 
        } else if (update.hasMessage()) {
        	Long user_id = update.getMessage().getFrom().getId();
            if(adminUserId.equals(user_id)){
                FlowResource flowResource = new FlowResource();
                flowResource.setStartDateOfValidity(Calendar.getInstance());
                flowResource.setName(adminTitle);
                flowResource.setDescription(update.getMessage().getText());
                flowResource.setRecipientID("@"+channelId);

                flowResources.add(flowResource);
            }
        }
    }

    private void logMessage(Update update) {
        log.info(update.getMessage() != null ? update.getMessage().getFrom().getUserName() + " " + update.getMessage().getText() : "Message received");
    }

    @Autowired
    public TelegramAIAssistantBotSensor(@Autowired Set<IFlowActuator<FlowResource>> flowActuators) {
        setFlowActuators(new HashSet<>(flowActuators));
        for (IFlowActuator<FlowResource> flowActuator : flowActuators){
            if(flowActuator.getClass() != SoffBlogFlowActuator.class && flowActuator.getClass() != GistFlowActuator.class){
                getFlowActuators().remove(flowActuator);
            } else {
                log.info(String.format("TelegramAIAssistantBotSensor has been created with an Actuator of type [%s]",
                        flowActuator.getClass().getName()));
            }
        }
    }

    @Override
    public void onChange(Set<FlowResource> resources) throws FlowException {
        log.info(String.format("onChange has been called with [%d] resources", resources.size()));
        for(IFlowActuator flowActuator : getFlowActuators()){
            flowActuator.doAction(resources);
        }
        log.info(String.format("onChange has been successfully terminated with [%d] resources",
                resources.size()));
    }

    @Override
    public Calendar getLastChangeDate() {
        return Calendar.getInstance();
    }
}
