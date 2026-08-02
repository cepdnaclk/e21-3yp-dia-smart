package com.diasmart.springapi.devices.dto;

import java.time.LocalDate;

public class DeviceKitRegistrationRequestDTO {

    private String kitUid;
    private String buyerFullName;
    private String nic;
    private String contactNumber;
    private String address;
    private LocalDate purchaseDate;

    private String outerGatewayId;
    private String innerUnitId;
    private String penUnitId;
    private String glucoseMeterId;

    public String getKitUid() {
        return kitUid;
    }

    public void setKitUid(String kitUid) {
        this.kitUid = kitUid;
    }

    public String getBuyerFullName() {
        return buyerFullName;
    }

    public void setBuyerFullName(String buyerFullName) {
        this.buyerFullName = buyerFullName;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

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
