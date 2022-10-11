package com.gist.flow.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

import java.util.Calendar;

@Data
@Configuration
@Profile("soffblog")
@ConfigurationProperties("rssflow")
public class RssFlowSensorConfiguration {
    private Calendar lastChangeDate;
    private String rssLink;
    private String nodeElementCategory;
    private String nodeElementEntry;
    private String specialCharsToRemove;
    private Resource rssFile;
    private Integer rssSensorTimeout;
}
