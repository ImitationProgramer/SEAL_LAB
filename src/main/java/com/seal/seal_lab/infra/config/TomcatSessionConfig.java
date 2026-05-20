package com.seal.seal_lab.infra.config;

import org.apache.catalina.session.StandardManager;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatSessionConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> disableTomcatSessionPersistence() {
        return factory -> factory.addContextCustomizers(context -> {
            StandardManager manager = new StandardManager();
            // Prevent Tomcat from loading or storing serialized sessions across restarts.
            manager.setPathname(null);
            context.setManager(manager);
        });
    }
}
