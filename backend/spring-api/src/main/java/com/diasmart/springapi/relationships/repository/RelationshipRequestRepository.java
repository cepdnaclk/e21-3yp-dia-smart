package com.diasmart.springapi.relationships.repository;

import com.diasmart.springapi.relationships.entity.RelationshipRequest;
import com.diasmart.springapi.shared.enums.RelationshipStatus;
import com.diasmart.springapi.shared.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRequestRepository extends JpaRepository<RelationshipRequest, Long> {

    /**
     * Finds all incoming requests for a target user with a specific status (e.g., PENDING).
     */
    List<RelationshipRequest> findByTargetUserIdAndStatusOrderByCreatedAtDesc(
            Long targetUserId,
            RelationshipStatus status);

    /**
     * Finds all requests sent by a requester, ordered by creation date descending.
     */
    List<RelationshipRequest> findByRequesterUserIdOrderByCreatedAtDesc(Long requesterUserId);

    /**
     * Inherited from JpaRepository, declared explicitly for clarity and self-documentation.
     */
    @Override
    Optional<RelationshipRequest> findById(Long requestId);

    /**
     * Checks whether a request already exists between the same requester, target user, patient,
     * relationship type and status (usually PENDING).
     */
    boolean existsByRequesterUserIdAndTargetUserIdAndPatientIdAndRelationshipRoleAndStatus(
            Long requesterUserId,
            Long targetUserId,
            Long patientId,
            RelationshipType relationshipRole,
            RelationshipStatus status);

    /**
     * Finds relationship requests for a patient with a specific status (e.g., ACTIVE/ACCEPTED).
     */
    List<RelationshipRequest> findByPatientIdAndStatusOrderByCreatedAtDesc(
            Long patientId,
            RelationshipStatus status);

    /**
     * Finds relationship requests for a user in either role (requester or target) with a specific status.
     */
    @Query("SELECT r FROM RelationshipRequest r WHERE r.status = :status AND (r.requesterUserId = :userId OR r.targetUserId = :userId) ORDER BY r.createdAt DESC")
    List<RelationshipRequest> findActiveRequestsForUser(
            @Param("userId") Long userId,
            @Param("status") RelationshipStatus status);
}
