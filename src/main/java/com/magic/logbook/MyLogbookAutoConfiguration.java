package com.magic.logbook;

import com.magic.logbook.config.HttpClientConfiguration;
import com.magic.logbook.config.LogbookConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@Import({LogbookConfiguration.class, HttpClientConfiguration.class})
@PropertySource(value = {"classpath:" + MyLogbookAutoConfiguration.MY_LOGBOOK + ".yml",
        "classpath:" + MyLogbookAutoConfiguration.MY_LOGBOOK + "-${spring.profiles.active}.yml"},
        factory = YamlPropertySourceFactory.class)
public class MyLogbookAutoConfiguration {

    public static final String MY_LOGBOOK = "my-logbook";


}
