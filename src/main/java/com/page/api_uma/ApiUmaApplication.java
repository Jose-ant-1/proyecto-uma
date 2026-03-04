package com.page.api_uma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
@EnableScheduling
public class ApiUmaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiUmaApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        // 1. Creamos la factoría con los timeouts primero
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 segundos
        factory.setReadTimeout(10000);

        // 2. Creamos el RestTemplate con esa factoría
        RestTemplate restTemplate = new RestTemplate(factory);

        // 3. Añadimos el interceptor para el User-Agent
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            request.getHeaders().set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
            return execution.execute(request, body);
        });

        return restTemplate;
    }

}
