package com.gist.flow.sensor.impl;

import com.gist.flow.actuator.IFlowActuator;
import com.gist.flow.configuration.RssFlowSensorConfiguration;
import com.gist.flow.exception.FlowException;
import com.gist.flow.model.entity.FlowResource;
import com.gist.flow.sensor.IFlowSensor;
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import lombok.Data;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

@Data
@Slf4j
@Service
@Profile("default")
public class RssFlowSensor implements IFlowSensor<FlowResource> {
    private IFlowActuator<FlowResource> rssFlowActuator;
    private Calendar lastChangeDate;

    @Autowired
    private RssFlowSensorConfiguration configuration;

    @Autowired
    public RssFlowSensor(IFlowActuator<FlowResource> rssFlowActuator) {
        log.info(String.format("RSSFlowSensor has been created with an Actuator of type [%s]",
                rssFlowActuator.getClass().getName()));
        this.rssFlowActuator = rssFlowActuator;
    }

    @Override
    public void onChange(Set<FlowResource> resources) throws FlowException {
        log.info(String.format("RSSFlowSensor onChange has been called with [%d] resources", resources.size()));
        rssFlowActuator.doAction(resources);
        lastChangeDate = Calendar.getInstance();
        log.info(String.format("RSSFlowSensor onChange has been successfully terminated with [%d] resources",
                resources.size()));
    }

    @Scheduled(fixedRateString = "${rssflow.rssSensorTimeout}")
    public void startSensor() {
        log.info(String.format("RSSFlowSensor has been started with an Actuator of type [%s]",
                rssFlowActuator.getClass().getName()));
        try {
            Calendar now = Calendar.getInstance();
            log.info(String.format("RSSFlowSensor - Start check at [%s]", now.getTime()));
            Set<FlowResource> resources = new HashSet<FlowResource>();
            ArrayList<SyndFeed> feeds = (ArrayList<SyndFeed>) getRSSFeedByCategory();
            FlowResource resource;
            Calendar lastDate = getLastChangeDate();
            log.info("LastChangeDate : " + new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(lastDate.getTime()));
            for (SyndFeed feed : feeds) {
                List<SyndEntry> entries = feed.getEntries();
                for (SyndEntry entry : entries) {
                    try {
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
                        String minimizedContent = entry.getLink().length() >= 30 ? entry.getLink().substring(0, 30)
                                : entry.getLink();
                        content.append("<br><a href='" + entry.getLink() + "'>(" + minimizedContent + "...)</a>");
                        resource.setDescription(content.toString());
                        if ((lastDate == null)
                                || (lastDate != null && resource.getStartDateOfValidity().compareTo(lastDate) > 0
                                && !resources.contains(resource)
                                && resource.getStartDateOfValidity().compareTo(now) <= 0)) {
                            resources.add(resource);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }
                }
            }
            log.info("RSSFlowSensor - Check completed");
            if (!resources.isEmpty()) {
                onChange(resources);
            } else {
                log.info("RSSFlowSensor - No changes founded");
            }
        } catch (FlowException e) {
            log.error(e.getMessage());
        }

        long nextTimeUpdate = configuration.getRssSensorTimeout() + System.currentTimeMillis();
        Calendar nextTimeUpdateCalendar = Calendar.getInstance();
        nextTimeUpdateCalendar.setTimeInMillis(nextTimeUpdate);
        log.info(String.format("RSSFlowSensor next update [%s]", nextTimeUpdateCalendar.getTime()));
    }

    /**
     * Method to get a list of RSS Feed divided by Category
     *
     * @return
     * @throws FlowException
     */
    @SuppressWarnings("unchecked")
    protected Collection<SyndFeed> getRSSFeedByCategory() throws FlowException {
        List<SyndFeed> output = new ArrayList<SyndFeed>();
        // get urls by category
        Map<String, List<String>> rssfeedByCategory;
        try {
            rssfeedByCategory = getRSSFeedURLByCategory();
        } catch (IOException | JDOMException | IllegalArgumentException e) {
            throw new FlowException(e);
        }

        // source Feed
        URL url = null;
        XmlReader reader = null;
        SyndFeed feedRead = null;
        SyndFeed newFeed = null;
        for (String rssCategory : rssfeedByCategory.keySet()) {
            // create new feed
            newFeed = new SyndFeedImpl();
            newFeed.setTitle(rssCategory);
            newFeed.setDescription(rssCategory);
            newFeed.setPublishedDate(new Date());
            newFeed.setFeedType("rss_2.0"); // set the type of your feed
            newFeed.setLanguage("fr");

            for (String rssEntryUrl : rssfeedByCategory.get(rssCategory)) {
                try {
                    // source Feed
                    url = new URL(rssEntryUrl);
                    reader = new XmlReader(url);
                    // Create Parser RSS
                    feedRead = new SyndFeedInput().build(reader);
                } catch (FeedException | IOException | IllegalArgumentException e) {
                    log.error(rssEntryUrl + ">>" + e.getMessage());
                    continue;
                }

                newFeed.setLink(configuration.getRssLink());
                // add entries to the new feed
                newFeed.getEntries().addAll(feedRead.getEntries());
            }
            // add new feed to the list
            output.add(newFeed);
        }

        return output;
    }

    @Override
    public Calendar getLastChangeDate() {
        if (lastChangeDate == null) {
            lastChangeDate = Calendar.getInstance();
        }
        return lastChangeDate;
    }

    /**
     * @param lastChangeDate the lastChangeDate to set
     */
    public void setLastChangeDate(Calendar lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }

    /**
     * @return the rssFlowActuator
     */
    public IFlowActuator<FlowResource> getRssFlowActuator() {
        return rssFlowActuator;
    }

    /**
     * Method to get the RSS feed URLs divided by category
     *
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    @SuppressWarnings("rawtypes")
    public Map<String, List<String>> getRSSFeedURLByCategory() throws IOException, JDOMException {
        Map<String, List<String>> result = new HashMap<String, List<String>>();

        SAXBuilder builder = new SAXBuilder();
        Document document = (Document) builder.build(configuration.getRssFile().getInputStream());
        Element rootNode = document.getRootElement();
        List listCategories = rootNode.getChildren(configuration.getNodeElementCategory());

        Element categoryNode = null;
        String categoryName = null;
        List listEntries = null;
        Element entryNode = null;
        String entryURL = null;
        List<String> current = null;
        // Loop on categories
        for (int i = 0; i < listCategories.size(); i++) {
            categoryNode = (Element) listCategories.get(i);
            categoryName = categoryNode.getAttributeValue("name");

            listEntries = categoryNode.getChildren(configuration.getNodeElementEntry());
            // Loop on entries
            for (int j = 0; j < listEntries.size(); j++) {
                entryNode = (Element) listEntries.get(j);
                entryURL = entryNode.getAttributeValue("url");

                // add values to the map
                if (categoryName != null && entryURL != null) {
                    current = result.get(categoryName);
                    if (current == null) {
                        current = new ArrayList<String>();
                    }
                    current.add(entryURL);
                    result.put(categoryName, current);
                }

            }
        }
        return result;
    }

    /**
     * Method to get a unique file name for the file containing the RSS Feed
     *
     * @param category
     * @return
     */
    public static String getUniqueRSSFeedFileName(String category) {
        return category.replaceAll(" ", "_");
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
