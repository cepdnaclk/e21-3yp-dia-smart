package com.diasmart.springapi.mqtt.service;

import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttService {

    @Value("${mqtt.broker.ssl}")
    private String broker;

    @Value("${mqtt.client.id}")
    private String clientId;

    private final MqttConnectOptions options;
    private MqttClient client;

    public MqttService(MqttConnectOptions options) {
        this.options = options;
    }

    @PostConstruct
    public void init() {
        connect();
    }

    public synchronized void connect() {
        try {
            if (client == null) {
                client = new MqttClient(broker, clientId);
            }
            if (!client.isConnected()) {
                System.out.println("MqttService connecting to AWS IoT Core...");
                client.connect(options);
                System.out.println("MqttService connected to AWS IoT Core.");
            }
        } catch (MqttException e) {
            System.err.println("MqttService connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void publish(String topic, String payload) {
        publish(topic, payload, 1, false);
    }

    public void publish(String topic, String payload, int qos, boolean retained) {
        if (client == null || !client.isConnected()) {
            connect();
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(qos);
            message.setRetained(retained);
            client.publish(topic, message);
            System.out.println("Published MQTT message to " + topic);
        } catch (MqttException e) {
            System.err.println("Failed to publish to " + topic);
            e.printStackTrace();
            throw new RuntimeException("MQTT publish failed", e);
        }
    }

    public void subscribe(String topic) {
        subscribe(topic, (receivedTopic, message) -> {
        });
    }

    public void subscribe(String topic, IMqttMessageListener messageListener) {
        if (client == null || !client.isConnected()) {
            connect();
        }
        try {
            client.subscribe(topic, 1, messageListener);
            System.out.println("Subscribed to " + topic);
        } catch (MqttException e) {
            System.err.println("Failed to subscribe to " + topic);
            e.printStackTrace();
        }
    }

    public synchronized void reconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            System.err.println("Failed to disconnect MQTT client before reconnect: " + e.getMessage());
        }

        connect();
    }
}
