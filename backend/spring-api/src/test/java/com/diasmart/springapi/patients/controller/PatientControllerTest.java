package com.diasmart.springapi.patients.controller;

import com.diasmart.springapi.common.responses.ApiResponse;
import com.diasmart.springapi.devices.dto.DeviceKitActivationResponseDTO;
import com.diasmart.springapi.devices.dto.PatientDeviceActivationRequestDTO;
import com.diasmart.springapi.devices.service.DeviceService;
import com.diasmart.springapi.patients.service.PatientService;
import com.diasmart.springapi.shared.security.RequestIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientControllerTest {

    @Test
    void activateDeviceKitShouldPassResolvedIpAndReturnActivationResponse() {
        PatientService patientService = mock(PatientService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RequestIpResolver requestIpResolver = mock(RequestIpResolver.class);
        PatientController controller = new PatientController(
                patientService,
                deviceService,
                requestIpResolver);
        PatientDeviceActivationRequestDTO request = activationRequest();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        DeviceKitActivationResponseDTO serviceResponse = activationResponse("ACTIVATED");

        when(requestIpResolver.resolve(httpRequest))
                .thenReturn("203.0.113.10");
        when(deviceService.activateDeviceKit(25L, request, "203.0.113.10"))
                .thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<DeviceKitActivationResponseDTO>> response =
                controller.activateDeviceKit(25L, request, httpRequest);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Device kit activated successfully", response.getBody().getMessage());
        assertEquals(serviceResponse, response.getBody().getData());
        verify(deviceService).activateDeviceKit(25L, request, "203.0.113.10");
    }

    @Test
    void activateDeviceKitShouldReturnAlreadyActiveMessage() {
        PatientService patientService = mock(PatientService.class);
        DeviceService deviceService = mock(DeviceService.class);
        RequestIpResolver requestIpResolver = mock(RequestIpResolver.class);
        PatientController controller = new PatientController(
                patientService,
                deviceService,
                requestIpResolver);
        PatientDeviceActivationRequestDTO request = activationRequest();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        DeviceKitActivationResponseDTO serviceResponse = activationResponse("ALREADY_ACTIVE");

        when(requestIpResolver.resolve(httpRequest))
                .thenReturn("203.0.113.10");
        when(deviceService.activateDeviceKit(25L, request, "203.0.113.10"))
                .thenReturn(serviceResponse);

        ResponseEntity<ApiResponse<DeviceKitActivationResponseDTO>> response =
                controller.activateDeviceKit(25L, request, httpRequest);

        assertNotNull(response.getBody());
        assertEquals("Device kit is already active for this patient", response.getBody().getMessage());
    }

    private PatientDeviceActivationRequestDTO activationRequest() {
        PatientDeviceActivationRequestDTO request = new PatientDeviceActivationRequestDTO();
        request.setOuterGatewayId("OUT-1");
        request.setInnerUnitId("INN-1");
        request.setPenUnitId("PEN-1");
        request.setGlucoseMeterId("GLU-1");
        return request;
    }

    private DeviceKitActivationResponseDTO activationResponse(String status) {
        DeviceKitActivationResponseDTO response = new DeviceKitActivationResponseDTO();
        response.setPatientId(25L);
        response.setKitId(77L);
        response.setKitUid("KIT-1");
        response.setActivationStatus(status);
        response.setActivatedAt(OffsetDateTime.parse("2026-08-02T12:00:00Z"));
        response.setDevices(new DeviceKitActivationResponseDTO.ActivationDevicesDTO());
        return response;
    }
}
