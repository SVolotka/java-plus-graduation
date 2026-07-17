package ru.practicum.collector.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcPortEurekaRegistrar {

    static final String GRPC_PORT_METADATA_KEY = "gRPC_port";

    private final EurekaRegistration eurekaRegistration;

    @EventListener
    public void registerActualPort(GrpcServerStartedEvent event) {
        int actualPort = event.getPort();
        String port = Integer.toString(actualPort);
        eurekaRegistration.getInstanceConfig().getMetadataMap()
                .put(GRPC_PORT_METADATA_KEY, port);
        eurekaRegistration.getApplicationInfoManager().getInfo().getMetadata()
                .put(GRPC_PORT_METADATA_KEY, port);
        eurekaRegistration.getApplicationInfoManager().getInfo().setIsDirty();
        log.info("Registered actual gRPC port in Eureka metadata");
    }
}