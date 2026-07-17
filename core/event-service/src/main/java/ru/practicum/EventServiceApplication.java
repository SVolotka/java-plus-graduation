package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"ru.practicum", "ru.yandex.practicum.common", "ru.practicum.statsclient"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.yandex.practicum.common.feignClient")
public class EventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}