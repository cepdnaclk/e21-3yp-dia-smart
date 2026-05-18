package com.diasmart.springapi.patients.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "patient_uuid")
    private UUID patientUuid;

    @Column(name = "nic")
    private String nic;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "emergency_contact_number")
    private String emergencyContactNumber;

    @Column(name = "address")
    private String address;

    @Column(name = "diabetes_type")
    private String diabetesType;

    @Column(name = "target_glucose_min_mg_dl")
    private BigDecimal targetGlucoseMinMgDl;

    @Column(name = "target_glucose_max_mg_dl")
    private BigDecimal targetGlucoseMaxMgDl;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Long getPatientId() {
        return patientId;
    }

    public UUID getPatientUuid() {
        return patientUuid;
    }

    public String getNic() {
        return nic;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getDiabetesType() {
        return diabetesType;
    }

    public BigDecimal getTargetGlucoseMinMgDl() {
        return targetGlucoseMinMgDl;
    }

    public BigDecimal getTargetGlucoseMaxMgDl() {
        return targetGlucoseMaxMgDl;
    }

    public Boolean getActive() {
        return active;
    }

    public String getNotes() {
        return notes;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public void setPatientUuid(UUID patientUuid) {
        this.patientUuid = patientUuid;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setDiabetesType(String diabetesType) {
        this.diabetesType = diabetesType;
    }

    public void setTargetGlucoseMinMgDl(BigDecimal targetGlucoseMinMgDl) {
        this.targetGlucoseMinMgDl = targetGlucoseMinMgDl;
    }

    public void setTargetGlucoseMaxMgDl(BigDecimal targetGlucoseMaxMgDl) {
        this.targetGlucoseMaxMgDl = targetGlucoseMaxMgDl;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}