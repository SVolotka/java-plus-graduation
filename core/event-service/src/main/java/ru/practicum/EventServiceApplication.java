package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = {"ru.practicum", "ru.yandex.practicum"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "ru.yandex.practicum.common.feignClient")
@ComponentScan(basePackages = {"ru.practicum", "ru.practicum.statsclient"})
public class EventServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}