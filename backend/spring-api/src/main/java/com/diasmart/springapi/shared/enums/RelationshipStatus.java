package com.diasmart.springapi.shared.enums;

/**
 * RelationshipStatus tracks the lifecycle of a relationship request or active
 * relationship.
 */
public enum RelationshipStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    REVOKED
}