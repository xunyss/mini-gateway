package io.xunyss.minigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MiniGatewayApplication {

    @Bean
    public ContentCachingFilter contentCachingFilter() {
        return new ContentCachingFilter(false);
    }


    public static void main(String[] args) {
        SpringApplication.run(MiniGatewayApplication.class, args);
    }
}
