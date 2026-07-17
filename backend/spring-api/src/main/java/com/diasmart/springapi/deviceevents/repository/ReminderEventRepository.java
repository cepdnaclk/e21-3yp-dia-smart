package com.diasmart.springapi.deviceevents.repository;

import com.diasmart.springapi.deviceevents.entity.ReminderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderEventRepository extends JpaRepository<ReminderEvent, Long> {
}
