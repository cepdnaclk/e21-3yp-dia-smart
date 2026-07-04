import type {
  CareTeamMember,
  RelationshipRequest,
} from "../types/careTeam";

export const careTeamService = {
  // TODO: Integrate assigned doctor API when Milestone 4 backend endpoints are available.
  getDoctors: async (): Promise<CareTeamMember[]> => {
    return [];
  },

  // TODO: Integrate caregiver API when Milestone 4 backend endpoints are available.
  getCaregivers: async (): Promise<CareTeamMember[]> => {
    return [];
  },

  // TODO: Integrate relationship request API when Milestone 4 backend endpoints are available.
  getRelationshipRequests: async (): Promise<
    RelationshipRequest[]
  > => {
    return [];
  },
};
