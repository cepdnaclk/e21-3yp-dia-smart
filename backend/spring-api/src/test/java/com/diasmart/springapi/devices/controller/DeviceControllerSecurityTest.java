package com.diasmart.springapi.devices.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DeviceControllerSecurityTest {

    @Test
    void genericDeviceControllerShouldRequireAdminRole() {
        PreAuthorize preAuthorize = DeviceController.class.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }
}
