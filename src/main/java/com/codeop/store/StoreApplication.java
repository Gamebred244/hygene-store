package com.codeop.store;

import com.codeop.store.config.AppDatabaseProperties;
import com.codeop.store.config.DatabaseInitializerListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppDatabaseProperties.class)
@ConfigurationPropertiesScan
public class StoreApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(StoreApplication.class);
        application.addListeners(new DatabaseInitializerListener());
        application.run(args);
    }

}
