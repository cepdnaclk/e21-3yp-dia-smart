package com.diasmart.springapi.mqtt.subscriber;

import com.diasmart.springapi.mqtt.dto.TelemetryPayloadDTO;
import com.diasmart.springapi.mqtt.service.TelemetryProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttSubscriber {

    @Value("${mqtt.broker.ssl}")
    private String broker;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.client.id}")
    private String clientId;

    private final MqttConnectOptions options;

    private final ObjectMapper mapper =
            new ObjectMapper();

    @Autowired
    private TelemetryProcessingService
            telemetryProcessingService;

    public MqttSubscriber(
            MqttConnectOptions options
    ) {
        this.options = options;
    }

    @PostConstruct
    public void init() {
        try {
            System.out.println("MQTT subscriber starting...");

            MqttClient client =
                    new MqttClient(
                            broker,
                            clientId
                    );

            client.connect(options);
            System.out.println("Connected to AWS IoT Core");

            client.subscribe(
                    topic,
                    (t, message) -> {
                        String payload =
                                new String(
                                        message.getPayload(),
                                        StandardCharsets.UTF_8
                                );

                        System.out.println(
                                "MQTT message received"
                        );
                        System.out.println(payload);

                        try {
                            TelemetryPayloadDTO dto =
                                    mapper.readValue(
                                            payload,
                                            TelemetryPayloadDTO.class
                                    );

                            telemetryProcessingService
                                    .process(dto, payload, t);

                        } catch (Exception e) {
                            System.out.println(
                                    "MQTT message processing error"
                            );
                            e.printStackTrace();
                        }
                    }
            );

        } catch (Exception e) {
            System.out.println("MQTT connection error");
            e.printStackTrace();
        }
    }
}
