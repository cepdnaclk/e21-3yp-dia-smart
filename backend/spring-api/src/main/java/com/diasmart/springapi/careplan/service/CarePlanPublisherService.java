package com.diasmart.springapi.careplan.service;

import com.diasmart.springapi.careplan.entity.CarePlanDeliveryStatus;
import com.diasmart.springapi.careplan.entity.CarePlanSnapshot;
import com.diasmart.springapi.careplan.repository.CarePlanDeliveryStatusRepository;
import com.diasmart.springapi.careplan.repository.CarePlanSnapshotRepository;
import com.diasmart.springapi.mqtt.service.MqttService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class CarePlanPublisherService {

    private static final int MQTT_QOS_ONE = 1;

    private final MqttService mqttService;
    private final CarePlanSnapshotRepository snapshotRepository;
    private final CarePlanDeliveryStatusRepository deliveryRepository;

    public CarePlanPublisherService(
            MqttService mqttService,
            CarePlanSnapshotRepository snapshotRepository,
            CarePlanDeliveryStatusRepository deliveryRepository) {
        this.mqttService = mqttService;
        this.snapshotRepository = snapshotRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public CarePlanSnapshot publish(CarePlanSnapshot snapshot) {
        CarePlanDeliveryStatus delivery = new CarePlanDeliveryStatus();
        delivery.setSnapshotId(snapshot.getSnapshotId());
        delivery.setOuterDeviceId(snapshot.getOuterDeviceId());
        delivery.setStatus("PENDING");
        delivery = deliveryRepository.save(delivery);

        RuntimeException lastException = null;
        String topic = "diasmart/devices/" + snapshot.getOuterDeviceUid() + "/care-plan";

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                mqttService.publish(topic, snapshot.getPayload(), MQTT_QOS_ONE, false);

                OffsetDateTime publishedAt = OffsetDateTime.now();
                snapshot.setStatus("PUBLISHED");
                snapshot.setPublishedAt(publishedAt);
                snapshotRepository.save(snapshot);

                delivery.setStatus("PUBLISHED");
                delivery.setPublishedAt(publishedAt);
                deliveryRepository.save(delivery);
                return snapshot;
            } catch (RuntimeException ex) {
                lastException = ex;
                delivery.setStatus("FAILED");
                delivery.setResponseMessage(ex.getMessage());
                deliveryRepository.save(delivery);
            }
        }

        snapshot.setStatus("FAILED");
        snapshotRepository.save(snapshot);
        throw lastException == null ? new IllegalStateException("Care Plan publish failed") : lastException;
    }
}
