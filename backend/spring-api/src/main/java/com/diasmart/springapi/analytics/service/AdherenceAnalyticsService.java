package com.diasmart.springapi.analytics.service;

import com.diasmart.springapi.analytics.dto.AdherenceAnalyticsResponse;
import com.diasmart.springapi.analytics.dto.AdherenceEntry;
import com.diasmart.springapi.analytics.dto.DailyAdherenceBreakdown;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdherenceAnalyticsService {

    private final DoseScheduleRepository doseScheduleRepository;
    private final DoseEventRepository doseEventRepository;
    private final AuthorizationService authorizationService;

    public AdherenceAnalyticsService(
            DoseScheduleRepository doseScheduleRepository,
            DoseEventRepository doseEventRepository,
            AuthorizationService authorizationService) {
        this.doseScheduleRepository = doseScheduleRepository;
        this.doseEventRepository = doseEventRepository;
        this.authorizationService = authorizationService;
    }

    public AdherenceAnalyticsResponse getAdherenceAnalytics(
            Long patientId, LocalDate startDate, LocalDate endDate) {

        authorizationService.authorize(Permission.READ_ADHERENCE_ANALYTICS, patientId);

        // Fetch all active schedules for this patient
        List<DoseSchedule> activeSchedules = doseScheduleRepository.findByPatientIdAndActiveTrue(patientId);

        // Fetch all dose events in the full date range (UTC boundaries)
        OffsetDateTime rangeStart = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime rangeEnd = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<DoseEvent> eventsInRange = doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, rangeStart,
                rangeEnd);

        // Counters
        int totalScheduled = 0;
        int onTimeCount = 0;
        int lateCount = 0;
        int missedCount = 0;
        int unscheduledCount = 0;

        List<DailyAdherenceBreakdown> dailyBreakdowns = new ArrayList<>();

        // Loop day by day through the requested range
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {

            int dayOfWeek = day.getDayOfWeek().getValue(); // ISO: 1=Monday ... 7=Sunday

            // Filter schedules that apply on this day of week
            final LocalDate currentDay = day;
            List<DoseSchedule> schedulesForDay = activeSchedules.stream()
                    .filter(s -> scheduleAppliesToDay(s, dayOfWeek))
                    .toList();

            // Filter events that fall within this calendar day (UTC)
            OffsetDateTime dayStart = currentDay.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime dayEnd = currentDay.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            List<DoseEvent> eventsForDay = eventsInRange.stream()
                    .filter(e -> !e.getInjectedAt().isBefore(dayStart)
                            && e.getInjectedAt().isBefore(dayEnd))
                    .toList();

            Set<Long> matchedEventIds = new HashSet<>();
            List<AdherenceEntry> entries = new ArrayList<>();

            // For each schedule slot on this day, find the best matching event
            for (DoseSchedule schedule : schedulesForDay) {
                totalScheduled++;

                int earlyMinutes = schedule.getAllowedEarlyMinutes() != null
                        ? schedule.getAllowedEarlyMinutes()
                        : 60;
                int lateMinutes = schedule.getAllowedLateMinutes() != null
                        ? schedule.getAllowedLateMinutes()
                        : 120;

                OffsetDateTime scheduledAt = OffsetDateTime.of(
                        currentDay, schedule.getScheduledTime(), ZoneOffset.UTC);
                OffsetDateTime windowStart = scheduledAt.minusMinutes(earlyMinutes);
                OffsetDateTime windowEnd = scheduledAt.plusMinutes(lateMinutes);

                // Pick the event closest to the scheduled time within the window
                DoseEvent bestMatch = null;
                long bestDistance = Long.MAX_VALUE;
                for (DoseEvent event : eventsForDay) {
                    if (!event.getInjectedAt().isBefore(windowStart)
                            && !event.getInjectedAt().isAfter(windowEnd)) {
                        long distance = Math.abs(
                                ChronoUnit.MINUTES.between(event.getInjectedAt(), scheduledAt));
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestMatch = event;
                        }
                    }
                }

                AdherenceEntry entry = buildScheduledEntry(schedule, bestMatch, scheduledAt);

                if (bestMatch != null) {
                    matchedEventIds.add(bestMatch.getDoseEventId());
                    if (!bestMatch.getInjectedAt().isAfter(scheduledAt)) {
                        entry.setStatus("ON_TIME");
                        onTimeCount++;
                    } else {
                        entry.setStatus("LATE");
                        lateCount++;
                    }
                } else {
                    entry.setStatus("MISSED");
                    missedCount++;
                }

                entries.add(entry);
            }

            // Any event not matched to a schedule window is UNSCHEDULED
            for (DoseEvent event : eventsForDay) {
                if (!matchedEventIds.contains(event.getDoseEventId())) {
                    unscheduledCount++;
                    AdherenceEntry entry = new AdherenceEntry();
                    entry.setStatus("UNSCHEDULED");
                    entry.setDoseEventId(event.getDoseEventId());
                    entry.setInjectedAt(event.getInjectedAt());
                    entries.add(entry);
                }
            }

            DailyAdherenceBreakdown breakdown = new DailyAdherenceBreakdown();
            breakdown.setDate(currentDay);
            breakdown.setEntries(entries);
            dailyBreakdowns.add(breakdown);
        }

        double adherenceRate = totalScheduled > 0
                ? Math.round(((double) (onTimeCount + lateCount) / totalScheduled) * 10000.0) / 100.0
                : 0.0;

        AdherenceAnalyticsResponse response = new AdherenceAnalyticsResponse();
        response.setPatientId(patientId);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setTotalScheduled(totalScheduled);
        response.setOnTime(onTimeCount);
        response.setLate(lateCount);
        response.setMissed(missedCount);
        response.setUnscheduled(unscheduledCount);
        response.setAdherenceRate(adherenceRate);
        response.setDailyBreakdown(dailyBreakdowns);
        return response;
    }

    // Returns true if the schedule applies on the given ISO day-of-week (1=Mon,
    // 7=Sun)
    private boolean scheduleAppliesToDay(DoseSchedule schedule, int dayOfWeek) {
        if (schedule.getDaysOfWeek() == null || schedule.getDaysOfWeek().isBlank()) {
            return false;
        }
        return Arrays.stream(schedule.getDaysOfWeek().split(","))
                .map(String::trim)
                .anyMatch(d -> d.equals(String.valueOf(dayOfWeek)));
    }

    // Builds an AdherenceEntry pre-filled with schedule metadata
    private AdherenceEntry buildScheduledEntry(
            DoseSchedule schedule, DoseEvent matchedEvent, OffsetDateTime scheduledAt) {
        AdherenceEntry entry = new AdherenceEntry();
        entry.setScheduleId(schedule.getScheduleId());
        entry.setScheduleLabel(schedule.getScheduleLabel());
        entry.setScheduledTime(schedule.getScheduledTime());
        if (matchedEvent != null) {
            entry.setDoseEventId(matchedEvent.getDoseEventId());
            entry.setInjectedAt(matchedEvent.getInjectedAt());
        }
        return entry;
    }
}
