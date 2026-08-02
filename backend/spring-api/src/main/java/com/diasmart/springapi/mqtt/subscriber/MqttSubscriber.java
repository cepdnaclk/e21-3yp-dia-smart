package com.diasmart.springapi.mqtt.subscriber;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.careplan.service.CarePlanAckService;
import com.diasmart.springapi.deviceevents.service.DeviceTelemetryProcessingService;
import com.diasmart.springapi.mqtt.dto.CommandAckDTO;
import com.diasmart.springapi.mqtt.dto.TelemetryPayloadDTO;
import com.diasmart.springapi.mqtt.service.CommandAckProcessingService;
import com.diasmart.springapi.mqtt.service.MqttService;
import com.diasmart.springapi.mqtt.service.TelemetryProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
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
    private final String deviceTelemetryTopic = "diasmart/devices/+/telemetry";
    private final String deviceCommandAckTopic = "diasmart/devices/+/command-ack";
    private final int maxPayloadBytes = 8192;

    private final MqttService mqttService;
    private final AuditService auditService;
    private final TelemetryProcessingService telemetryProcessingService;
    private final CommandAckProcessingService commandAckProcessingService;
    private final CarePlanAckService carePlanAckService;
    private final DeviceTelemetryProcessingService deviceTelemetryProcessingService;

    private final ObjectMapper mapper;

    public MqttSubscriber(
            MqttService mqttService,
            AuditService auditService,
            TelemetryProcessingService telemetryProcessingService,
            CommandAckProcessingService commandAckProcessingService,
            CarePlanAckService carePlanAckService,
            DeviceTelemetryProcessingService deviceTelemetryProcessingService,
            ObjectMapper mapper) {
        this.mqttService = mqttService;
        this.auditService = auditService;
        this.telemetryProcessingService = telemetryProcessingService;
        this.commandAckProcessingService = commandAckProcessingService;
        this.carePlanAckService = carePlanAckService;
        this.deviceTelemetryProcessingService = deviceTelemetryProcessingService;
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        System.out.println("MQTT subscriber initializing...");

        // 1. Subscribe to telemetry
        mqttService.subscribe(telemetryTopic, (t, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            handleTelemetry(t, payload);
        });

        mqttService.subscribe(deviceTelemetryTopic, (t, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            handleTelemetry(t, payload);
        });

        // 2. Subscribe to command ACKs
        mqttService.subscribe(commandAckTopic, (t, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            handleCommandAck(t, payload);
        });

        mqttService.subscribe(deviceCommandAckTopic, (t, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            handleCommandAck(t, payload);
        });
    }

    private void handleTelemetry(String topic, String payload) {
        System.out.println("MQTT Telemetry received on " + topic);

        if (payload.getBytes(StandardCharsets.UTF_8).length > maxPayloadBytes) {
            auditService.logFailedPayloadValidation(null, null, null, null, null, topic, "Payload exceeds size limit", null);
            return;
        }

        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(payload);
        } catch (Exception e) {
            System.out.println("MQTT Telemetry parsing error");
            auditService.logFailedPayloadValidation(null, null, null, null, null, topic, e.getMessage(), payload);
            return;
        }

        try {
            if (deviceTelemetryProcessingService.supports(jsonNode)) {
                deviceTelemetryProcessingService.process(jsonNode, payload, topic);
                return;
            }

            TelemetryPayloadDTO dto = mapper.treeToValue(jsonNode, TelemetryPayloadDTO.class);
            telemetryProcessingService.process(dto, payload, topic);
        } catch (Exception e) {
            System.out.println("MQTT Telemetry processing error");
            e.printStackTrace();
        }
    }

    private void handleCommandAck(String topic, String payload) {
        System.out.println("MQTT Command ACK received on " + topic);

        JsonNode jsonNode;
        try {
            jsonNode = mapper.readTree(payload);
        } catch (Exception e) {
            System.out.println("MQTT Command ACK parsing error");
            e.printStackTrace();
            return;
        }

        try {
            if (jsonNode.hasNonNull("carePlanId")) {
                carePlanAckService.processAck(jsonNode);
                return;
            }

            CommandAckDTO dto = mapper.treeToValue(jsonNode, CommandAckDTO.class);
            commandAckProcessingService.processAck(dto, extractDeviceUidFromTopic(topic));
        } catch (Exception e) {
            System.out.println("MQTT Command ACK processing error");
            e.printStackTrace();
        }
    }

    private String extractDeviceUidFromTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }

        String[] parts = topic.split("/");
        for (int index = 0; index < parts.length - 1; index++) {
            if ("devices".equals(parts[index])) {
                return parts[index + 1];
            }
        }

        return null;
    }
}
