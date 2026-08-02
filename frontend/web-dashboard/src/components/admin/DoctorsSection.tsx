import UserDirectorySection from "./UserDirectorySection";
import type { AdminUserRecord } from "../../types/admin";

interface DoctorsSectionProps {
  users: AdminUserRecord[];
  onToggleStatus: (userId: number, currentActive: boolean) => void;
  onAddClick: () => void;
}

const DoctorsSection: React.FC<DoctorsSectionProps> = ({
  users,
  onToggleStatus,
  onAddClick
}) => {
  return (
    <UserDirectorySection
      title="Doctors Directory"
      description="Lists all doctor clinician accounts. Admins can manage clinical licensing flags, clinic associations, and assigned patient limits."
      addButtonLabel="Add Doctor"
      users={users}
      onToggleStatus={onToggleStatus}
      onAddClick={onAddClick}
    />
  );
};

export default DoctorsSection;
