package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.actuator.impl.SoffBlogFlowActuator;
import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import com.gist.flow.sensor.IFlowSensor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

@Data
@Slf4j
@Service
@Profile({"soffblog","gist"})
public class TelegramBotSensor implements IFlowSensor<FlowResource> {
    @Value("${telegram.bot.admin.id}")
    private Long adminUserId;

    @Value("${telegram.bot.admin.title}")
    private String title;

    private Set<IFlowActuator<FlowResource>> flowActuators = new HashSet<>();

    private HashSet<FlowResource> flowResources = new HashSet<>(1);

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
        if (update.hasMessage()) {
            Long user_id = update.getMessage().getFrom().getId();
            if(adminUserId.equals(user_id)){
                FlowResource flowResource = new FlowResource();
                flowResource.setStartDateOfValidity(Calendar.getInstance());
                flowResource.setName(title);
                flowResource.setDescription(update.getMessage().getText());

                flowResources.add(flowResource);
            }
        }
    }

    private void logMessage(Update update) {
        log.info(update.getMessage() != null ? update.getMessage().getFrom().getUserName() + " " + update.getMessage().getText() : "Message received");
    }

    @Autowired
    public TelegramBotSensor(@Autowired Set<IFlowActuator<FlowResource>> flowActuators) {
        setFlowActuators(new HashSet<>(flowActuators));
        for (IFlowActuator<FlowResource> flowActuator : flowActuators){
            if(flowActuator.getClass() != SoffBlogFlowActuator.class){
                getFlowActuators().remove(flowActuator);
            } else {
                log.info(String.format("TelegramBotSensor has been created with an Actuator of type [%s]",
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
