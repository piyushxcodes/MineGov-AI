import axios from "axios";

export const api = axios.create({
  baseURL: "http://127.0.0.1:8000",
  timeout: 10000,
});

export type Violation = {
  id: string;
  localId: string;

  violationType: string;
  description: string;
  observedCondition: string;
  severity: string;

  latitude: number | null;
  longitude: number | null;

  photoUri: string | null;
  videoUri: string | null;
  voiceUri: string | null;

  status: string;
  createdAt: number;
  updatedAt: number;

  assignedTo: string | null;
  assignedAt: number | null;

  slaDeadline: number | null;
  slaStatus: string;
  remainingSlaMs: number | null;

  escalationRequired: boolean;

  // AI
  aiRiskScore: number | null;
  aiRiskLevel: string | null;
  aiFinding: string | null;
  aiConfidence: number | null;

  aiDetections: {
  class: string;
  confidence: number;
  box: [number, number, number, number];
}[];

aiVisionFindings: string | null;
aiVisionScore: number | null;
};

export type ViolationsResponse = {
  count: number;
  violations: Violation[];
};

export async function getViolations(): Promise<ViolationsResponse> {
  const response = await api.get<ViolationsResponse>(
    "/api/v1/violations"
  );

  return response.data;
}

export async function assignViolation(
  violationId: string,
  assignedTo: string
) {
  const response = await api.patch(
    `/api/v1/violations/${violationId}/assign`,
    {
      assignedTo,
    }
  );

  return response.data;
}

export async function updateViolationStatus(
  violationId: string,
  status: string
) {
  const response = await api.patch(
    `/api/v1/violations/${violationId}/status`,
    {
      status,
    }
  );

  return response.data;
}