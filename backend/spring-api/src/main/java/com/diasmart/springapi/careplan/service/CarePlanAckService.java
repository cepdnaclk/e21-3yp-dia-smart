package com.diasmart.springapi.careplan.service;

import com.diasmart.springapi.careplan.entity.CarePlanDeliveryStatus;
import com.diasmart.springapi.careplan.entity.CarePlanSnapshot;
import com.diasmart.springapi.careplan.repository.CarePlanDeliveryStatusRepository;
import com.diasmart.springapi.careplan.repository.CarePlanSnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
public class CarePlanAckService {

    private final CarePlanSnapshotRepository snapshotRepository;
    private final CarePlanDeliveryStatusRepository deliveryRepository;

    public CarePlanAckService(
            CarePlanSnapshotRepository snapshotRepository,
            CarePlanDeliveryStatusRepository deliveryRepository) {
        this.snapshotRepository = snapshotRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public void processAck(JsonNode payload) {
        String carePlanId = text(payload, "carePlanId");
        Integer version = integer(payload, "version");

        if (carePlanId == null && version == null) {
            System.out.println("Care Plan ACK missing carePlanId/version. Ignoring.");
            return;
        }

        CarePlanSnapshot snapshot = carePlanId == null
                ? null
                : snapshotRepository.findByCarePlanUid(carePlanId).orElse(null);

        if (snapshot == null) {
            System.out.println("Care Plan ACK could not be matched: " + carePlanId);
            return;
        }

        if (version != null && !version.equals(snapshot.getVersion())) {
            System.out.println("Care Plan ACK version mismatch for " + carePlanId);
            return;
        }

        String status = normalizeStatus(text(payload, "status"));
        OffsetDateTime acknowledgedAt = parseTimestamp(text(payload, "timestamp"));

        snapshot.setStatus(status);
        snapshot.setAcknowledgedAt(acknowledgedAt);
        snapshotRepository.save(snapshot);

        CarePlanDeliveryStatus delivery = deliveryRepository
                .findTopBySnapshotIdAndOuterDeviceIdOrderByCreatedAtDesc(snapshot.getSnapshotId(), snapshot.getOuterDeviceId())
                .orElseGet(() -> {
                    CarePlanDeliveryStatus newDelivery = new CarePlanDeliveryStatus();
                    newDelivery.setSnapshotId(snapshot.getSnapshotId());
                    newDelivery.setOuterDeviceId(snapshot.getOuterDeviceId());
                    return newDelivery;
                });

        delivery.setStatus(status);
        delivery.setAcknowledgedAt(acknowledgedAt);
        delivery.setResponseMessage(text(payload, "message"));
        deliveryRepository.save(delivery);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "FAILED";
        }

        return status.trim().toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }

        try {
            return OffsetDateTime.parse(timestamp);
        } catch (Exception ignored) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    private String text(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private Integer integer(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        return node == null || !node.canConvertToInt() ? null : node.asInt();
    }
}
