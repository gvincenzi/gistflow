package com.gist.flow.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Calendar;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties("rssflow")
public class RssFlowSensorConfiguration {
    private Calendar lastChangeDate;
    private String rssLink;
    private String specialCharsToRemove;
    private List<String> feeds;
}
