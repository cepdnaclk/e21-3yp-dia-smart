package com.diasmart.springapi.relationships.repository;

import com.diasmart.springapi.relationships.entity.UserPatientAccess;
import com.diasmart.springapi.shared.enums.AccessStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPatientAccessRepository extends JpaRepository<UserPatientAccess, Long> {

        Optional<UserPatientAccess> findByUserIdAndPatientIdAndStatus(
                        Long userId,
                        Long patientId,
                        AccessStatus status);

        Optional<UserPatientAccess> findByUserIdAndPatientId(
                        Long userId,
                        Long patientId);

        boolean existsByUserIdAndPatientIdAndStatusAndCanViewTrue(
                        Long userId,
                        Long patientId,
                        AccessStatus status);

        boolean existsByUserIdAndPatientIdAndStatusAndCanAcknowledgeAlertsTrue(
                        Long userId,
                        Long patientId,
                        AccessStatus status);

        boolean existsByUserIdAndPatientIdAndStatusAndCanEditPrescriptionsTrue(
                        Long userId,
                        Long patientId,
                        AccessStatus status);

        List<UserPatientAccess> findByUserIdAndStatusAndCanViewTrue(
                        Long userId,
                        AccessStatus status);

        List<UserPatientAccess> findByUserIdAndStatusOrderByCreatedAtDesc(
                        Long userId,
                        AccessStatus status);

        List<UserPatientAccess> findByUserIdOrderByCreatedAtDesc(
                        Long userId);
}