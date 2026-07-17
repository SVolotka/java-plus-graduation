package ru.yandex.practicum.core.requestService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"ru.yandex.practicum", "ru.practicum.statsclient"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.yandex.practicum.common.feignClient")
@EnableAsync
public class RequestServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApplication.class, args);
    }
}