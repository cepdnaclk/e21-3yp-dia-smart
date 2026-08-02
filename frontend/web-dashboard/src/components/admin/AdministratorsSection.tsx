import UserDirectorySection from "./UserDirectorySection";
import type { AdminUserRecord } from "../../types/admin";

interface AdministratorsSectionProps {
  users: AdminUserRecord[];
  onToggleStatus: (userId: number, currentActive: boolean) => void;
  onAddClick: () => void;
}

const AdministratorsSection: React.FC<AdministratorsSectionProps> = ({
  users,
  onToggleStatus,
  onAddClick
}) => {
  return (
    <UserDirectorySection
      title="Administrators Directory"
      description="Lists all administrator accounts. Manage admin credentials, audit visibility rights, and system settings authorizations."
      addButtonLabel="Add Administrator"
      users={users}
      onToggleStatus={onToggleStatus}
      onAddClick={onAddClick}
    />
  );
};

export default AdministratorsSection;
