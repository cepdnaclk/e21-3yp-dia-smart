package com.diasmart.springapi.mqtt.subscriber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.OffsetDateTime;
@Component
public class MqttSubscriber {

    @Value("${mqtt.broker.ssl}")
    private String broker;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.client.id}")
    private String clientId;

    private final MqttConnectOptions options;

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private GlucoseReadingRepository repository;

    public MqttSubscriber(MqttConnectOptions options) {
        this.options = options;
    }

    @PostConstruct
    public void init() {

        try {

            System.out.println("🚀 MQTT Subscriber Starting...");

            MqttClient client =
                    new MqttClient(
                            broker,
                            clientId
                    );

            client.connect(options);

            System.out.println("✅ Connected to AWS IoT Core");

            client.subscribe(topic, (t, message) -> {

                String payload =
                        new String(message.getPayload());

                System.out.println("📩 MQTT MESSAGE RECEIVED");

                System.out.println(payload);

                JsonNode json =
                        mapper.readTree(payload);

                Long patientId =
                        json.get("patient")
                                .get("patientId")
                                .asLong();

                int glucose =
                        json.get("glucose")
                                .get("valueMgDl")
                                .asInt();

                GlucoseReading reading =
                        new GlucoseReading();

                reading.setPatientId(patientId);
                
                        reading.setGlucoseValueMgDl(
                        Double.valueOf(glucose)
                );
                reading.setMeasuredAt(
                        OffsetDateTime.now()
                );

                repository.save(reading);

                System.out.println("✅ Saved To RDS");

                System.out.println(
                        "Patient ID: " + patientId
                );

                System.out.println(
                        "Glucose: " + glucose
                );
            });

        } catch (Exception e) {

            System.out.println("❌ MQTT ERROR");

            e.printStackTrace();
        }
    }
}