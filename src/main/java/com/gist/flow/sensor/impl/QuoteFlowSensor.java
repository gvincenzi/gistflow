package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import com.gist.flow.sensor.IFlowSensor;
import com.gist.flow.service.MistralAIChatService;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

@Data
@Slf4j
@Service
@Profile("soffblog")
public class QuoteFlowSensor implements IFlowSensor<FlowResource> {
    private IFlowActuator<FlowResource> rssFlowActuator;

    @Value("${quote.feed}")
    private String feed;

    @Value("${quote.energybreak.title}")
    private String energyBreakTitle;

    @Value("${quote.energybreak.messageHTML}")
    private String energyBreakMessageHTML;
    
    @Autowired
    private MistralAIChatService mistralAIChatService;

    @Autowired
    public QuoteFlowSensor(IFlowActuator<FlowResource> soffBlogQuoteFlowActuator) {
        log.info(String.format("QuoteFlowSensor has been created with an Actuator of type [%s]",
                soffBlogQuoteFlowActuator.getClass().getName()));
        this.rssFlowActuator = soffBlogQuoteFlowActuator;
    }

    @Override
    public void onChange(Set<FlowResource> resources) throws FlowException {
        log.info(String.format("QuoteFlowSensor onChange has been called with [%d] resources", resources.size()));
        rssFlowActuator.doAction(resources);
        log.info(String.format("QuoteFlowSensor onChange has been successfully terminated with [%d] resources",
                resources.size()));
    }

    @Scheduled(cron = "0 0 6 ? * *")
    public void startSensor() {
        Set<FlowResource> resources = new HashSet<>();
        try (XmlReader reader = new XmlReader(new URL(feed))) {
            SyndFeed feed = new SyndFeedInput().build(reader);
            for (SyndEntry entry : feed.getEntries()) {
                FlowResource translatedResource = new FlowResource();
                translatedResource.setName(entry.getTitle());
                translatedResource.setDescription(mistralAIChatService.translateQuote(entry.getDescription().getValue(), "italian") + "\n\nFonte : " + entry.getUri());
                translatedResource.setStartDateOfValidity(Calendar.getInstance());
                resources.add(translatedResource);
            }

            onChange(resources);
        } catch (IOException | FeedException | FlowException e) {
            log.error(e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 ? * *")
    public void energyPause() {
        Set<FlowResource> resources = new HashSet<>();
        FlowResource resource = new FlowResource();
        resource.setName(energyBreakTitle);
        resource.setDescription(energyBreakMessageHTML);
        resource.setStartDateOfValidity(Calendar.getInstance());
        resources.add(resource);
        try {
            onChange(resources);
        } catch (FlowException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public Calendar getLastChangeDate() {
        return Calendar.getInstance();
    }
}
