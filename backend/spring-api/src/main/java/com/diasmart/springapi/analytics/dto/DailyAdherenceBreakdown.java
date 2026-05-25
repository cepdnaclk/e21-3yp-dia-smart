package com.diasmart.springapi.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyAdherenceBreakdown {

    private LocalDate date;
    private List<AdherenceEntry> entries;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<AdherenceEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<AdherenceEntry> entries) {
        this.entries = entries;
    }
}
