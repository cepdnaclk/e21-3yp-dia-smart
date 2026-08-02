import UserDirectorySection from "./UserDirectorySection";
import type { AdminUserRecord } from "../../types/admin";

interface PatientsSectionProps {
  users: AdminUserRecord[];
  onToggleStatus: (userId: number, currentActive: boolean) => void;
  onAddClick: () => void;
}

const PatientsSection: React.FC<PatientsSectionProps> = ({
  users,
  onToggleStatus,
  onAddClick
}) => {
  return (
    <UserDirectorySection
      title="Patients Directory"
      description="Lists all patient user accounts. Admins can view diagnostic statuses, toggle account states, and manage device mappings."
      addButtonLabel="Add Patient"
      users={users}
      onToggleStatus={onToggleStatus}
      onAddClick={onAddClick}
    />
  );
};

export default PatientsSection;
