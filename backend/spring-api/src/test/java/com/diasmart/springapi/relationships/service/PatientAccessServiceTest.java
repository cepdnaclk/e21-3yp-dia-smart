package com.diasmart.springapi.relationships.service;

import com.diasmart.springapi.relationships.repository.UserPatientAccessRepository;
import com.diasmart.springapi.shared.enums.AccessRole;
import com.diasmart.springapi.shared.enums.AccessStatus;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientAccessServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserPatientAccessRepository userPatientAccessRepository;

    @InjectMocks
    private PatientAccessService patientAccessService;

    @Test
    void requireCanManagePatientDevicesShouldAllowPatientSelfAccess() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.PATIENT, true));
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndAccessRoleAndStatus(
                7L,
                25L,
                AccessRole.SELF,
                AccessStatus.ACTIVE))
                .thenReturn(true);

        assertDoesNotThrow(() -> patientAccessService.requireCanManagePatientDevices(25L));
    }

    @Test
    void requireCanManagePatientDevicesShouldRejectPathVariableManipulation() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.PATIENT, true));
        when(userPatientAccessRepository.existsByUserIdAndPatientIdAndAccessRoleAndStatus(
                7L,
                99L,
                AccessRole.SELF,
                AccessStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> patientAccessService.requireCanManagePatientDevices(99L));
    }

    @Test
    void requireCanManagePatientDevicesShouldRejectCaregiverWithOnlyViewAuthority() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.CAREGIVER, true));

        assertThrows(
                AccessDeniedException.class,
                () -> patientAccessService.requireCanManagePatientDevices(25L));

        verify(userPatientAccessRepository, never())
                .existsByUserIdAndPatientIdAndAccessRoleAndStatus(
                        7L,
                        25L,
                        AccessRole.SELF,
                        AccessStatus.ACTIVE);
    }

    @Test
    void requireCanManagePatientDevicesShouldRejectDoctorWithOnlyViewAuthority() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.DOCTOR, true));

        assertThrows(
                AccessDeniedException.class,
                () -> patientAccessService.requireCanManagePatientDevices(25L));
    }

    @Test
    void requireCanManagePatientDevicesShouldRejectInactiveUser() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.ADMIN, false));

        assertThrows(
                AccessDeniedException.class,
                () -> patientAccessService.requireCanManagePatientDevices(25L));
    }

    @Test
    void requireCanManagePatientDevicesShouldAllowActiveAdmin() {
        when(currentUserService.getCurrentUser())
                .thenReturn(user(UserRole.ADMIN, true));

        assertDoesNotThrow(() -> patientAccessService.requireCanManagePatientDevices(25L));
    }

    private AppUser user(UserRole role, boolean active) {
        AppUser user = new AppUser();
        user.setUserId(7L);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
