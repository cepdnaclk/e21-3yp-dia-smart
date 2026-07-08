package com.diasmart.springapi.devices.dto;

public class PatientDeviceActivationRequestDTO {
    private String outerGatewayId;
    private String innerUnitId;
    private String penUnitId;
    private String glucoseMeterId;

    public String getOuterGatewayId() {
        return outerGatewayId;
    }

    public void setOuterGatewayId(String outerGatewayId) {
        this.outerGatewayId = outerGatewayId;
    }

    public String getInnerUnitId() {
        return innerUnitId;
    }

    public void setInnerUnitId(String innerUnitId) {
        this.innerUnitId = innerUnitId;
    }

    public String getPenUnitId() {
        return penUnitId;
    }

    public void setPenUnitId(String penUnitId) {
        this.penUnitId = penUnitId;
    }

    public String getGlucoseMeterId() {
        return glucoseMeterId;
    }

    public void setGlucoseMeterId(String glucoseMeterId) {
        this.glucoseMeterId = glucoseMeterId;
    }
}
