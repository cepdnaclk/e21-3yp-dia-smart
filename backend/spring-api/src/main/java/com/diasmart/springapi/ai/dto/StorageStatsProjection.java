package com.diasmart.springapi.ai.dto;

public interface StorageStatsProjection {
    Long getCount();
    Double getAverage();
    Double getMinimum();
    Double getMaximum();
}
