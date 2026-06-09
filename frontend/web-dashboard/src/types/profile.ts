export interface Profile {
  userId: number;
  userUuid: string;
  displayName: string;
  email: string;
  role: string;
  contactNumber: string;
  active: boolean;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}