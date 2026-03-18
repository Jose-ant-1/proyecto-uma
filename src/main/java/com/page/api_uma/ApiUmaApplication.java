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
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(10000);

        RestTemplate restTemplate = new RestTemplate(factory);

        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

            // Cabeceras cruciales para saltar protecciones básicas
            request.getHeaders().set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            request.getHeaders().set("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");
            request.getHeaders().set("Accept-Encoding", "gzip, deflate, br");
            request.getHeaders().set("Connection", "keep-alive");
            request.getHeaders().set("Sec-Ch-Ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"");
            request.getHeaders().set("Sec-Ch-Ua-Mobile", "?0");
            request.getHeaders().set("Sec-Ch-Ua-Platform", "\"Windows\"");

            return execution.execute(request, body);
        });

        return restTemplate;
    }

}
