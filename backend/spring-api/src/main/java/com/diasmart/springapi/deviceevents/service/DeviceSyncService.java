package com.diasmart.springapi.deviceevents.service;

import com.diasmart.springapi.careplan.service.CarePlanService;
import com.diasmart.springapi.deviceconfig.service.impl.DeviceConfigurationServiceImpl;
import com.diasmart.springapi.deviceevents.entity.DeviceSyncRequest;
import com.diasmart.springapi.deviceevents.repository.DeviceSyncRequestRepository;
import com.diasmart.springapi.devices.entity.Device;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceSyncService {

    private final DeviceSyncRequestRepository syncRequestRepository;
    private final CarePlanService carePlanService;
    private final DeviceConfigurationServiceImpl deviceConfigurationService;

    public DeviceSyncService(
            DeviceSyncRequestRepository syncRequestRepository,
            CarePlanService carePlanService,
            DeviceConfigurationServiceImpl deviceConfigurationService) {
        this.syncRequestRepository = syncRequestRepository;
        this.carePlanService = carePlanService;
        this.deviceConfigurationService = deviceConfigurationService;
    }

    @Transactional
    public void recordAndResync(Device outerDevice, String eventId, String requestType) {
        if (outerDevice == null) {
            return;
        }

        DeviceSyncRequest syncRequest = new DeviceSyncRequest();
        syncRequest.setEventId(eventId);
        syncRequest.setOuterDeviceId(outerDevice.getDeviceId());
        syncRequest.setOuterDeviceUid(outerDevice.getDeviceUid());
        syncRequest.setPatientId(outerDevice.getPatientId());
        syncRequest.setRequestType(requestType == null || requestType.isBlank() ? "DEVICE_SYNC_REQUEST" : requestType);
        syncRequestRepository.save(syncRequest);

        try {
            deviceConfigurationService.sendConfigurationForDeviceSync(outerDevice);
        } catch (RuntimeException ex) {
            System.out.println("Device configuration sync publish skipped: " + ex.getMessage());
        }

        try {
            carePlanService.resendCurrentForDevice(outerDevice);
        } catch (RuntimeException ex) {
            System.out.println("Care Plan sync publish skipped: " + ex.getMessage());
        }
    }
}
