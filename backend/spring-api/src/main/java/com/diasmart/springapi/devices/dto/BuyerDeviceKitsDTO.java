package com.diasmart.springapi.devices.dto;

import java.util.List;

public class BuyerDeviceKitsDTO {
    
    private DeviceResponseDTO.BuyerDTO buyer;
    private int purchaseCount;
    private List<DeviceKitDTO> kits;

    public DeviceResponseDTO.BuyerDTO getBuyer() {
        return buyer;
    }

    public void setBuyer(DeviceResponseDTO.BuyerDTO buyer) {
        this.buyer = buyer;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(int purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public List<DeviceKitDTO> getKits() {
        return kits;
    }

    public void setKits(List<DeviceKitDTO> kits) {
        this.kits = kits;
    }
}
