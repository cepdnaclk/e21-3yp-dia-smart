package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TelemetryPayloadDTO {

    private String eventId;

    private String eventType;

    private String timestamp;

    private Integer schemaVersion;

    private Long sequenceNumber;

    private Boolean replayedEvent;

    private String trigger;

    private PatientDTO patient;

    private GatewayDTO gateway;

    private StorageTelemetryDTO storage;

    private GlucoseDTO glucose;

    private DoseDTO dose;

    private InventoryTelemetryDTO inventory;

    private BatteryTelemetryDTO battery;

    // =========================
    // GETTERS
    // =========================

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public Boolean getReplayedEvent() {
        return replayedEvent;
    }

    public String getTrigger() {
        return trigger;
    }

    public PatientDTO getPatient() {
        return patient;
    }

    public GatewayDTO getGateway() {
        return gateway;
    }

    public StorageTelemetryDTO getStorage() {
        return storage;
    }

    public GlucoseDTO getGlucose() {
        return glucose;
    }

    public DoseDTO getDose() {
        return dose;
    }

    public InventoryTelemetryDTO getInventory() {
        return inventory;
    }

    public BatteryTelemetryDTO getBattery() {
        return battery;
    }

    // =========================
    // SETTERS
    // =========================

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public void setSequenceNumber(Long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setReplayedEvent(Boolean replayedEvent) {
        this.replayedEvent = replayedEvent;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public void setPatient(PatientDTO patient) {
        this.patient = patient;
    }

    public void setGateway(GatewayDTO gateway) {
        this.gateway = gateway;
    }

    public void setStorage(StorageTelemetryDTO storage) {
        this.storage = storage;
    }

    public void setGlucose(GlucoseDTO glucose) {
        this.glucose = glucose;
    }

    public void setDose(DoseDTO dose) {
        this.dose = dose;
    }

    public void setInventory(
            InventoryTelemetryDTO inventory
    ) {
        this.inventory = inventory;
    }

    public void setBattery(
            BatteryTelemetryDTO battery
    ) {
        this.battery = battery;
    }
}
