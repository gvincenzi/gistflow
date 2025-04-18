package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.configuration.RssFlowSensorConfiguration;
import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import com.gist.flow.sensor.IFlowSensor;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Slf4j
@Service
@Profile("default")
public class RssFlowSensor implements IFlowSensor<FlowResource> {
    private Set<IFlowActuator<FlowResource>> rssFlowActuators;
    private Calendar lastChangeDate = Calendar.getInstance();

    @Autowired
    private RssFlowSensorConfiguration configuration;

    @Autowired
    public RssFlowSensor(Set<IFlowActuator<FlowResource>> rssFlowActuators) {
        this.rssFlowActuators = rssFlowActuators;
        for(IFlowActuator flowActuator : rssFlowActuators){
            log.info(String.format("RssFlowSensor has been created with an Actuator of type [%s]",
                    flowActuator.getClass().getName()));
        }
    }

    @Override
    public void onChange(Set<FlowResource> resources) throws FlowException {
        log.info(String.format("RssFlowSensor onChange has been called with [%d] resources", resources.size()));
        for(IFlowActuator flowActuator : rssFlowActuators){
            flowActuator.doAction(resources);
        }
        setLastChangeDate(Calendar.getInstance());
        log.info(String.format("RssFlowSensor onChange has been successfully terminated with [%d] resources",
                resources.size()));
    }

    @Scheduled(cron = "${rssflow.cron}")
    public void startSensor() {
        log.info("RssFlowSensor has been started");
        Calendar now = Calendar.getInstance();
        log.info(String.format("RssFlowSensor - Start check at [%s]", now.getTime()));
        Set<FlowResource> resources = new HashSet<FlowResource>();

        FlowResource resource;
        Calendar lastDate = getLastChangeDate();
        log.debug("LastChangeDate : " + new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(lastDate.getTime()));

        for (String feedURL : configuration.getFeeds()) {
            try (XmlReader reader = new XmlReader(new URL(feedURL))) {
                SyndFeed feed = new SyndFeedInput().build(reader);
                log.debug(feed.getTitle());
                for (SyndEntry entry : feed.getEntries()) {
                    if (entry.getPublishedDate() == null || entry.getDescription() == null) {
                        continue;
                    }
                    resource = new FlowResource();
                    resource.setName(cleanString(entry.getTitle()));
                    if (entry.getPublishedDate().after(Calendar.getInstance().getTime())) {
                        resource.setStartDateOfValidity(Calendar.getInstance());
                    } else {
                        resource.getStartDateOfValidity().setTime(entry.getPublishedDate());
                    }
                    StringBuffer content = new StringBuffer();
                    SyndContent description = entry.getDescription();
                    List<SyndEnclosure> enclosures = entry.getEnclosures();
                    for (SyndEnclosure enclosure : enclosures) {
                        if (enclosure.getType().contains("image/jpeg") && enclosure.getUrl().length() > 0) {
                            content.append("<img src='" + enclosure.getUrl() + "'/><br>");
                        }
                    }
                    content.append(description.getValue());
                    String link = entry.getLink().replaceAll(".+http", "http").trim();
                    String minimizedContent = link.length() >= 30 ? link.substring(0, 30)
                            : link;
                    content.append("<br><a href='" + link + "'>(" + minimizedContent + "...)</a>");
                    resource.setDescription(content.toString());
                    if ((lastDate == null)
                            || (lastDate != null && resource.getStartDateOfValidity().compareTo(lastDate) > 0
                            && !resources.contains(resource)
                            && resource.getStartDateOfValidity().compareTo(now) <= 0)) {
                        resources.add(resource);
                    }
                }
            } catch (IOException | FeedException | IllegalArgumentException e) {
                log.error(e.getMessage());
                continue;
            }
        }

        log.info("RssFlowSensor - Check completed");
        if (!resources.isEmpty()) {
            try {
                onChange(resources);
            } catch (FlowException e) {
                log.error(e.getMessage());
            }
        } else {
            log.info("RssFlowSensor - No changes founded");
        }
    }

    /**
     * Method to replace all special chars
     *
     * @param s String
     */
    public String cleanString(String s) {
        s = s.replaceAll(String.valueOf("\'"), "'");
        for (char specialChar : configuration.getSpecialCharsToRemove().toCharArray()) {
            s = s.replaceAll(String.valueOf(specialChar), StringUtils.EMPTY);
        }

        return s.toUpperCase();
    }
}
