package com.diasmart.springapi.devices.dto;

public class DeviceReplayStatisticsDTO {

    private long totalMqttEvents;
    private long replayedEvents;
    private long duplicateEvents;

    public long getTotalMqttEvents() {
        return totalMqttEvents;
    }

    public void setTotalMqttEvents(long totalMqttEvents) {
        this.totalMqttEvents = totalMqttEvents;
    }

    public long getReplayedEvents() {
        return replayedEvents;
    }

    public void setReplayedEvents(long replayedEvents) {
        this.replayedEvents = replayedEvents;
    }

    public long getDuplicateEvents() {
        return duplicateEvents;
    }

    public void setDuplicateEvents(long duplicateEvents) {
        this.duplicateEvents = duplicateEvents;
    }
}
