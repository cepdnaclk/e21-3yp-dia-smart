import UserDirectorySection from "./UserDirectorySection";
import type { AdminUserRecord } from "../../types/admin";

interface CaregiversSectionProps {
  users: AdminUserRecord[];
  onToggleStatus: (userId: number, currentActive: boolean) => void;
  onAddClick: () => void;
}

const CaregiversSection: React.FC<CaregiversSectionProps> = ({
  users,
  onToggleStatus,
  onAddClick
}) => {
  return (
    <UserDirectorySection
      title="Caregivers Directory"
      description="Lists all caregiver support accounts. Admins can view assigned patient relationships and caregiver verification files."
      addButtonLabel="Add Caregiver"
      users={users}
      onToggleStatus={onToggleStatus}
      onAddClick={onAddClick}
    />
  );
};

export default CaregiversSection;
