package com.diasmart.springapi.analytics.service;

import com.diasmart.springapi.analytics.dto.AdherenceAnalyticsResponse;
import com.diasmart.springapi.analytics.dto.AdherenceEntry;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdherenceAnalyticsServiceTest {

    @Mock
    private DoseScheduleRepository doseScheduleRepository;

    @Mock
    private DoseEventRepository doseEventRepository;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private AdherenceAnalyticsService service;

    @Test
    void shouldThrowAccessDeniedWhenUnauthorized() {
        // Arrange
        Long patientId = 1L;
        LocalDate startDate = LocalDate.of(2026, 7, 10);
        LocalDate endDate = LocalDate.of(2026, 7, 10);

        doThrow(new AccessDeniedException("Unauthorized access"))
                .when(authorizationService)
                .authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> 
                service.getAdherenceAnalytics(patientId, startDate, endDate));

        verify(authorizationService, times(1))
                .authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        verifyNoInteractions(doseScheduleRepository);
        verifyNoInteractions(doseEventRepository);
    }

    @Test
    void shouldReturnZeroAdherenceWhenNoSchedules() {
        // Arrange
        Long patientId = 1L;
        LocalDate startDate = LocalDate.of(2026, 7, 10);
        LocalDate endDate = LocalDate.of(2026, 7, 10);

        OffsetDateTime expectedStart = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(Collections.emptyList());
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(Collections.emptyList());

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(patientId, response.getPatientId());
        assertEquals(startDate, response.getStartDate());
        assertEquals(endDate, response.getEndDate());
        assertEquals(0, response.getTotalScheduled());
        assertEquals(0.0, response.getAdherenceRate());
        assertEquals(1, response.getDailyBreakdown().size());
        assertTrue(response.getDailyBreakdown().get(0).getEntries().isEmpty());

        verify(doseScheduleRepository, times(1)).findByPatientIdAndActiveTrue(patientId);
        verify(doseEventRepository, times(1)).findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd);
    }

    @Test
    void shouldMarkAllAsMissedWhenNoEvents() {
        // Arrange
        Long patientId = 1L;
        LocalDate startDate = LocalDate.of(2026, 7, 10); // Friday (5)
        LocalDate endDate = LocalDate.of(2026, 7, 10);

        OffsetDateTime expectedStart = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduleLabel("Morning Insulin");
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("5"); // Friday
        schedule.setAllowedEarlyMinutes(30);
        schedule.setAllowedLateMinutes(60);
        schedule.setActive(true);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(Collections.emptyList());

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, startDate, endDate);

        // Assert
        assertEquals(1, response.getTotalScheduled());
        assertEquals(1, response.getMissed());
        assertEquals(0, response.getOnTime());
        assertEquals(0, response.getLate());
        assertEquals(0.0, response.getAdherenceRate());
        
        AdherenceEntry entry = response.getDailyBreakdown().get(0).getEntries().get(0);
        assertEquals("MISSED", entry.getStatus());
        assertEquals(10L, entry.getScheduleId());
        assertNull(entry.getDoseEventId());
    }

    @Test
    void shouldApplyDefaultFallbacksForNullOffsets() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("5");
        schedule.setAllowedEarlyMinutes(null); // Fallback is 60m
        schedule.setAllowedLateMinutes(null);  // Fallback is 120m
        schedule.setActive(true);

        // Event is 55 minutes early (7:05 UTC)
        DoseEvent event = new DoseEvent();
        event.setDoseEventId(100L);
        event.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(7, 5), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(event));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert
        assertEquals(1, response.getTotalScheduled());
        assertEquals(1, response.getOnTime());
        assertEquals(100.0, response.getAdherenceRate());
        
        AdherenceEntry entry = response.getDailyBreakdown().get(0).getEntries().get(0);
        assertEquals("ON_TIME", entry.getStatus());
        assertEquals(100L, entry.getDoseEventId());
    }

    @Test
    void shouldClassifyOnTimeAndLate() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule1 = new DoseSchedule();
        schedule1.setScheduleId(10L);
        schedule1.setScheduledTime(LocalTime.of(8, 0));
        schedule1.setDaysOfWeek("5");
        schedule1.setAllowedEarlyMinutes(30);
        schedule1.setAllowedLateMinutes(60);
        schedule1.setActive(true);

        DoseSchedule schedule2 = new DoseSchedule();
        schedule2.setScheduleId(20L);
        schedule2.setScheduledTime(LocalTime.of(12, 0));
        schedule2.setDaysOfWeek("5");
        schedule2.setAllowedEarlyMinutes(30);
        schedule2.setAllowedLateMinutes(60);
        schedule2.setActive(true);

        // Event 1: 8:00 (On Time)
        DoseEvent event1 = new DoseEvent();
        event1.setDoseEventId(100L);
        event1.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(8, 0), ZoneOffset.UTC));

        // Event 2: 12:15 (Late)
        DoseEvent event2 = new DoseEvent();
        event2.setDoseEventId(200L);
        event2.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(12, 15), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule1, schedule2));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(event1, event2));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert
        assertEquals(2, response.getTotalScheduled());
        assertEquals(1, response.getOnTime());
        assertEquals(1, response.getLate());
        assertEquals(100.0, response.getAdherenceRate());

        List<AdherenceEntry> entries = response.getDailyBreakdown().get(0).getEntries();
        assertEquals(2, entries.size());

        AdherenceEntry entry1 = entries.stream().filter(e -> e.getScheduleId().equals(10L)).findFirst().orElseThrow();
        assertEquals("ON_TIME", entry1.getStatus());
        assertEquals(100L, entry1.getDoseEventId());

        AdherenceEntry entry2 = entries.stream().filter(e -> e.getScheduleId().equals(20L)).findFirst().orElseThrow();
        assertEquals("LATE", entry2.getStatus());
        assertEquals(200L, entry2.getDoseEventId());
    }

    @Test
    void shouldSelectClosestEventInWindow() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("5");
        schedule.setAllowedEarlyMinutes(30);
        schedule.setAllowedLateMinutes(60);
        schedule.setActive(true);

        // Event A: 8:15 (15m offset)
        DoseEvent eventA = new DoseEvent();
        eventA.setDoseEventId(100L);
        eventA.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(8, 15), ZoneOffset.UTC));

        // Event B: 7:55 (5m offset) - Closer match
        DoseEvent eventB = new DoseEvent();
        eventB.setDoseEventId(200L);
        eventB.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(7, 55), ZoneOffset.UTC));

        // Event C: 8:25 (25m offset) - Further away (Tests the false branch of distance < bestDistance)
        DoseEvent eventC = new DoseEvent();
        eventC.setDoseEventId(300L);
        eventC.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(8, 25), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(eventA, eventB, eventC));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert
        assertEquals(1, response.getTotalScheduled());
        assertEquals(1, response.getOnTime()); // Event B is 7:55 (ON_TIME)
        assertEquals(2, response.getUnscheduled()); // Event A and C are unmatched
        assertEquals(100.0, response.getAdherenceRate());

        List<AdherenceEntry> entries = response.getDailyBreakdown().get(0).getEntries();
        assertEquals(3, entries.size());

        AdherenceEntry scheduledEntry = entries.stream()
                .filter(e -> "ON_TIME".equals(e.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(200L, scheduledEntry.getDoseEventId());

        assertTrue(entries.stream().anyMatch(e -> "UNSCHEDULED".equals(e.getStatus()) && e.getDoseEventId().equals(100L)));
        assertTrue(entries.stream().anyMatch(e -> "UNSCHEDULED".equals(e.getStatus()) && e.getDoseEventId().equals(300L)));
    }

    @Test
    void shouldMarkExtraEventsAsUnscheduled() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(Collections.emptyList());

        DoseEvent event = new DoseEvent();
        event.setDoseEventId(500L);
        event.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(15, 0), ZoneOffset.UTC));

        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(event));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert
        assertEquals(0, response.getTotalScheduled());
        assertEquals(1, response.getUnscheduled());
        assertEquals(0.0, response.getAdherenceRate());

        AdherenceEntry entry = response.getDailyBreakdown().get(0).getEntries().get(0);
        assertEquals("UNSCHEDULED", entry.getStatus());
        assertEquals(500L, entry.getDoseEventId());
    }

    @Test
    void shouldBypassLoopWhenStartDateAfterEndDate() {
        // Arrange
        Long patientId = 1L;
        LocalDate startDate = LocalDate.of(2026, 7, 12);
        LocalDate endDate = LocalDate.of(2026, 7, 10);

        OffsetDateTime expectedStart = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(Collections.emptyList());
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(Collections.emptyList());

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, startDate, endDate);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalScheduled());
        assertEquals(0.0, response.getAdherenceRate());
        assertTrue(response.getDailyBreakdown().isEmpty());
    }

    @Test
    void shouldHandleMalformedDaysOfWeekSafely() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("malformed,string"); // No match for dayOfWeek 5
        schedule.setActive(true);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(Collections.emptyList());

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert
        assertEquals(0, response.getTotalScheduled());
        assertEquals(0.0, response.getAdherenceRate());
        assertTrue(response.getDailyBreakdown().get(0).getEntries().isEmpty());
    }

    @Test
    void shouldMatchDoseEventExactlyOnAllowedEarlyWindowBoundary() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("5");
        schedule.setAllowedEarlyMinutes(10);
        schedule.setAllowedLateMinutes(10);
        schedule.setActive(true);

        // Event exactly on windowStart (7:50:00)
        DoseEvent eventEarly = new DoseEvent();
        eventEarly.setDoseEventId(100L);
        eventEarly.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(7, 50), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(eventEarly));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert: Early boundary matches and is ON_TIME
        assertEquals(1, response.getOnTime());
        assertEquals(0, response.getMissed());
    }

    @Test
    void shouldMatchDoseEventExactlyOnAllowedLateWindowBoundary() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("5");
        schedule.setAllowedEarlyMinutes(10);
        schedule.setAllowedLateMinutes(10);
        schedule.setActive(true);

        // Event exactly on windowEnd (8:10:00)
        DoseEvent eventLate = new DoseEvent();
        eventLate.setDoseEventId(200L);
        eventLate.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(8, 10), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(eventLate));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert: Late boundary matches and is LATE
        assertEquals(1, response.getLate());
        assertEquals(0, response.getMissed());
    }

    @Test
    void shouldVerifyExclusiveWindowBoundaries() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("5");
        schedule.setAllowedEarlyMinutes(10);
        schedule.setAllowedLateMinutes(10);
        schedule.setActive(true);

        // Event 1 second before windowStart (7:49:59)
        DoseEvent eventBefore = new DoseEvent();
        eventBefore.setDoseEventId(100L);
        eventBefore.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(7, 49, 59), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(eventBefore));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert
        assertEquals(1, response.getMissed());
        assertEquals(1, response.getUnscheduled());
    }

    @Test
    void shouldHandleYearBoundaryCrossover() {
        // Arrange
        Long patientId = 1L;
        LocalDate startDate = LocalDate.of(2026, 12, 31); // Thursday (4)
        LocalDate endDate = LocalDate.of(2027, 1, 1);     // Friday (5)

        OffsetDateTime expectedStart = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("4,5"); // Runs on Thursday and Friday
        schedule.setAllowedEarlyMinutes(30);
        schedule.setAllowedLateMinutes(60);
        schedule.setActive(true);

        // Event on Dec 31
        DoseEvent event1 = new DoseEvent();
        event1.setDoseEventId(100L);
        event1.setInjectedAt(OffsetDateTime.of(startDate, LocalTime.of(8, 0), ZoneOffset.UTC));

        // Event on Jan 1
        DoseEvent event2 = new DoseEvent();
        event2.setDoseEventId(200L);
        event2.setInjectedAt(OffsetDateTime.of(endDate, LocalTime.of(8, 5), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(event1, event2));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, startDate, endDate);

        // Assert
        assertEquals(2, response.getTotalScheduled());
        assertEquals(1, response.getOnTime());
        assertEquals(1, response.getLate());
        assertEquals(100.0, response.getAdherenceRate());
        assertEquals(2, response.getDailyBreakdown().size());
    }

    @Test
    void shouldThrowNullPointerForNullDates() {
        // Arrange
        Long patientId = 1L;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
                service.getAdherenceAnalytics(patientId, null, null));
    }

    @Test
    void shouldRetainFirstEventWhenEquidistant() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("5");
        schedule.setAllowedEarlyMinutes(30);
        schedule.setAllowedLateMinutes(30);
        schedule.setActive(true);

        // Event 1: 7:50 (10 minutes early) - First in list
        DoseEvent event1 = new DoseEvent();
        event1.setDoseEventId(100L);
        event1.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(7, 50), ZoneOffset.UTC));

        // Event 2: 8:10 (10 minutes late) - Equidistant, second in list
        DoseEvent event2 = new DoseEvent();
        event2.setDoseEventId(200L);
        event2.setInjectedAt(OffsetDateTime.of(day, LocalTime.of(8, 10), ZoneOffset.UTC));

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(List.of(event1, event2));

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert: Asserts that event1 is selected because distance check uses strict inequality (<)
        assertEquals(1, response.getOnTime());
        assertEquals(0, response.getLate());
        
        AdherenceEntry matchedEntry = response.getDailyBreakdown().get(0).getEntries().stream()
                .filter(e -> "ON_TIME".equals(e.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(100L, matchedEntry.getDoseEventId()); // Event 1 matched

        AdherenceEntry unmatchedEntry = response.getDailyBreakdown().get(0).getEntries().stream()
                .filter(e -> "UNSCHEDULED".equals(e.getStatus()))
                .findFirst().orElseThrow();
        assertEquals(200L, unmatchedEntry.getDoseEventId()); // Event 2 unmatched
    }

    @Test
    void shouldMatchScheduleWhenDaysOfWeekContainsSpaces() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10); // Friday (5)

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek(" 1, 3, 5 "); // Spaces around comma-delimited Friday
        schedule.setActive(true);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(Collections.emptyList());

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert: Confirm that schedule is active and evaluated because spaces are trimmed
        assertEquals(1, response.getTotalScheduled());
        assertEquals(1, response.getMissed());
    }

    @Test
    void shouldIgnoreScheduleWhenDaysOfWeekIsNull() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10);

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek(null); // NULL days of week
        schedule.setActive(true);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(Collections.emptyList());

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert: Confirm that schedule with null daysOfWeek is skipped
        assertEquals(0, response.getTotalScheduled());
        assertTrue(response.getDailyBreakdown().get(0).getEntries().isEmpty());
    }

    @Test
    void shouldIgnoreScheduleWhenDaysOfWeekIsBlank() {
        // Arrange
        Long patientId = 1L;
        LocalDate day = LocalDate.of(2026, 7, 10);

        OffsetDateTime expectedStart = day.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime expectedEnd = day.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        DoseSchedule schedule = new DoseSchedule();
        schedule.setScheduleId(10L);
        schedule.setScheduledTime(LocalTime.of(8, 0));
        schedule.setDaysOfWeek("   "); // BLANK days of week
        schedule.setActive(true);

        doNothing().when(authorizationService).authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(schedule));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, expectedStart, expectedEnd))
                .thenReturn(Collections.emptyList());

        // Act
        AdherenceAnalyticsResponse response = service.getAdherenceAnalytics(patientId, day, day);

        // Assert: Confirm that schedule with blank daysOfWeek is skipped
        assertEquals(0, response.getTotalScheduled());
        assertTrue(response.getDailyBreakdown().get(0).getEntries().isEmpty());
    }
}
