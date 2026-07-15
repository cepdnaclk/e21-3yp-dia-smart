package com.diasmart.springapi.mqtt.subscriber;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.mqtt.dto.CommandAckDTO;
import com.diasmart.springapi.mqtt.dto.TelemetryPayloadDTO;
import com.diasmart.springapi.mqtt.service.CommandAckProcessingService;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.diasmart.springapi.mqtt.service.TelemetryProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttSubscriber {

    @Value("${mqtt.topic}")
    private String telemetryTopic;

    private final String commandAckTopic = "diasmart/v1/devices/+/command-ack";

    private final MqttService mqttService;
    private final AuditService auditService;
    private final TelemetryProcessingService telemetryProcessingService;
    private final CommandAckProcessingService commandAckProcessingService;

    private final ObjectMapper mapper = new ObjectMapper();

    public MqttSubscriber(
            MqttService mqttService,
            AuditService auditService,
            TelemetryProcessingService telemetryProcessingService,
            CommandAckProcessingService commandAckProcessingService) {
        this.mqttService = mqttService;
        this.auditService = auditService;
        this.telemetryProcessingService = telemetryProcessingService;
        this.commandAckProcessingService = commandAckProcessingService;
    }

    @PostConstruct
    public void init() {
        System.out.println("MQTT subscriber initializing...");

        // 1. Subscribe to telemetry
        mqttService.subscribe(telemetryTopic, (t, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("MQTT Telemetry received on " + t);
            
            TelemetryPayloadDTO dto;
            try {
                dto = mapper.readValue(payload, TelemetryPayloadDTO.class);
            } catch (Exception e) {
                System.out.println("MQTT Telemetry parsing error");
                auditService.logFailedPayloadValidation(null, null, null, null, null, t, e.getMessage(), payload);
                return;
            }

            try {
                telemetryProcessingService.process(dto, payload, t);
            } catch (Exception e) {
                System.out.println("MQTT Telemetry processing error");
                e.printStackTrace();
            }
        });

        // 2. Subscribe to command ACKs
        mqttService.subscribe(commandAckTopic, (t, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("MQTT Command ACK received on " + t);

            CommandAckDTO dto;
            try {
                dto = mapper.readValue(payload, CommandAckDTO.class);
            } catch (Exception e) {
                System.out.println("MQTT Command ACK parsing error");
                e.printStackTrace();
                return;
            }

            try {
                commandAckProcessingService.processAck(dto);
            } catch (Exception e) {
                System.out.println("MQTT Command ACK processing error");
                e.printStackTrace();
            }
        });
    }
}
