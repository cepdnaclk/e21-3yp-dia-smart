package com.diasmart.springapi.deviceconfig.service.impl;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import com.diasmart.springapi.devices.entity.Device;

record WifiCommandPublishContext(
        DeviceCommand command,
        DeviceConfiguration configuration,
        Device outerDevice
) {
}
