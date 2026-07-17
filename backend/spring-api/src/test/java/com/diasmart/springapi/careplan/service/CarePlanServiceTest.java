package com.diasmart.springapi.careplan.service;

import com.diasmart.springapi.careplan.dto.CarePlanResponse;
import com.diasmart.springapi.careplan.entity.CarePlanSchedule;
import com.diasmart.springapi.careplan.entity.CarePlanSnapshot;
import com.diasmart.springapi.careplan.repository.CarePlanScheduleRepository;
import com.diasmart.springapi.careplan.repository.CarePlanSnapshotRepository;
import com.diasmart.springapi.devices.entity.Device;
import com.diasmart.springapi.devices.repository.DeviceRepository;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.prescriptions.entity.Prescription;
import com.diasmart.springapi.prescriptions.repository.PrescriptionRepository;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarePlanServiceTest {

    @Mock
    private CarePlanSnapshotRepository snapshotRepository;

    @Mock
    private CarePlanScheduleRepository carePlanScheduleRepository;

    @Mock
    private DoseScheduleRepository doseScheduleRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private CarePlanPublisherService publisherService;

    private CarePlanService service;

    @BeforeEach
    void setUp() {
        service = new CarePlanService(
                snapshotRepository,
                carePlanScheduleRepository,
                doseScheduleRepository,
                prescriptionRepository,
                deviceRepository,
                authorizationService,
                publisherService,
                new ObjectMapper()
        );
    }

    @Test
    void generateAndPublishShouldBuildVersionedCarePlanPayload() throws Exception {
        Device outer = new Device();
        outer.setDeviceId(1L);
        outer.setPatientId(10L);
        outer.setDeviceUid("OUTER-001");
        outer.setDeviceType("OUTER_GATEWAY");
        outer.setActive(true);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(100L);
        schedule.setPrescriptionId(5L);
        schedule.setPatientId(10L);
        schedule.setScheduleLabel("Morning dose");
        schedule.setDoseUnits(BigDecimal.TEN);
        schedule.setTargetTime(LocalTime.of(8, 0));
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setWindowStart(LocalTime.of(7, 30));
        schedule.setWindowEnd(LocalTime.of(8, 30));

        Prescription prescription = new Prescription();
        prescription.setPrescriptionId(5L);
        prescription.setPrescriptionName("Rapid Acting");

        when(deviceRepository.findFirstByPatientIdAndDeviceTypeAndActiveTrue(10L, "OUTER_GATEWAY"))
                .thenReturn(Optional.of(outer));
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(10L)).thenReturn(List.of(schedule));
        when(snapshotRepository.findTopByPatientIdOrderByVersionDesc(10L)).thenReturn(Optional.empty());
        when(prescriptionRepository.findById(5L)).thenReturn(Optional.of(prescription));
        when(snapshotRepository.save(any(CarePlanSnapshot.class))).thenAnswer(invocation -> {
            CarePlanSnapshot snapshot = invocation.getArgument(0);
            if (snapshot.getSnapshotId() == null) {
                snapshot.setSnapshotId(77L);
            }
            return snapshot;
        });
        when(carePlanScheduleRepository.save(any(CarePlanSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(publisherService.publish(any(CarePlanSnapshot.class))).thenAnswer(invocation -> {
            CarePlanSnapshot snapshot = invocation.getArgument(0);
            snapshot.setStatus("PUBLISHED");
            return snapshot;
        });

        CarePlanResponse response = service.generateAndPublish(10L);

        assertEquals("CP-10-1", response.getCarePlanId());
        assertEquals(1, response.getVersion());
        assertEquals("PUBLISHED", response.getStatus());

        ArgumentCaptor<CarePlanSnapshot> snapshotCaptor = ArgumentCaptor.forClass(CarePlanSnapshot.class);
        verify(publisherService).publish(snapshotCaptor.capture());

        var payload = new ObjectMapper().readTree(snapshotCaptor.getValue().getPayload());
        assertEquals("OUTER-001", payload.get("outerDeviceId").asText());
        assertEquals("SCH-100", payload.get("schedules").get(0).get("scheduleId").asText());
        assertEquals("07:30", payload.get("schedules").get(0).get("windowStart").asText());
        assertEquals("08:00", payload.get("schedules").get(0).get("targetTime").asText());
        assertEquals("08:30", payload.get("schedules").get(0).get("windowEnd").asText());
    }
}
