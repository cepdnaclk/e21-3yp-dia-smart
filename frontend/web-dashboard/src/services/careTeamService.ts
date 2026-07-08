import api from "./api";
import type {
  CreateRelationshipRequestDto,
  RelationshipRequestDto,
  RelationshipSummaryDto,
} from "../types/careTeam";

export const careTeamService = {
  async searchDoctors(query = ""): Promise<any[]> {
    const response = await api.get("/relationships/doctors", {
      params: { q: query },
    });
    return response.data.data;
  },

  async searchCaregivers(query = ""): Promise<any[]> {
    const response = await api.get("/relationships/caregivers", {
      params: { q: query },
    });
    return response.data.data;
  },

  async sendRequest(dto: CreateRelationshipRequestDto): Promise<RelationshipRequestDto> {
    const response = await api.post("/relationship-requests", dto);
    return response.data.data;
  },

  async getSentRequests(): Promise<RelationshipRequestDto[]> {
    const response = await api.get("/relationship-requests/sent");
    return response.data.data;
  },

  async getMyRelationships(): Promise<RelationshipSummaryDto[]> {
    const response = await api.get("/relationships/me");
    return response.data.data;
  },

  async revokeRelationship(requestId: number): Promise<RelationshipRequestDto> {
    const response = await api.patch(`/relationship-requests/${requestId}/revoke`);
    return response.data.data;
  },
};
