package com.magic.logbook.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties("logbook.custom.response")
public class LogbookCustomResponseProperties {

    private LogbookHeaderStrategy headerStrategy = LogbookHeaderStrategy.ALL;
    private List<String> customHeaders;

}
