export interface Violation {
  id: string;
  localId?: string;
  violationType: string;
  description: string;
  observedCondition: string;
  severity: string;
  latitude?: number | null;
  longitude?: number | null;
  photoUri?: string | null;
  videoUri?: string | null;
  voiceUri?: string | null;
  voiceTranscript?: string | null;
  voiceLanguage?: string | null;
  documentUri?: string | null;
  ocrText?: string | null;

  status: string;
  assignedTo?: string | null;
  assignedAt?: number | null;
  slaDeadline?: number | null;

  aiRiskScore?: number | null;
  aiRiskLevel?: string | null;
  aiFinding?: string | null;
  aiConfidence?: number | null;

  aiDetections?: Array<{
    class: string;
    confidence: number;
    box: number[];
  }>;

  aiVisionFindings?: string | null;
  aiVisionScore?: number | null;

  createdAt: number;
  updatedAt: number;
}

export interface AuditLog {
  id: string;
  violationId: string;
  action: string;
  actor?: string | null;
  oldStatus?: string | null;
  newStatus?: string | null;
  details?: string | null;
  timestamp: number;
  previousHash?: string | null;
  currentHash: string;
}

const API_BASE_URL = "https://minegov-backend.onrender.com";

export async function getViolations(): Promise<Violation[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/violations`
  );
  if (!response.ok) {
    throw new Error(
      `Failed to fetch violations: ${response.status}`
    );
  }
  return response.json();
}

export async function getViolation(
  violationId: string
): Promise<Violation> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/violations/${violationId}`
  );
  if (!response.ok) {
    throw new Error(
      `Failed to fetch violation: ${response.status}`
    );
  }
  return response.json();
}

export async function getAuditLogs(
  violationId: string
): Promise<AuditLog[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/violations/${violationId}/audit`
  );
  if (!response.ok) {
    throw new Error(
      `Failed to fetch audit logs: ${response.status}`
    );
  }
  return response.json();
}

export function getEvidenceUrl(
  uri?: string | null
): string | null {
  if (!uri) return null;
  if (
    uri.startsWith("http://") ||
    uri.startsWith("https://")
  ) {
    return uri;
  }
  return `${API_BASE_URL}${uri}`;
}

export function formatDate(
  timestamp?: number | null
): string {
  if (!timestamp) return "—";
  return new Date(timestamp).toLocaleString();
}

export function getSlaStatus(
  deadline?: number | null
): "OVERDUE" | "DUE_SOON" | "ON_TRACK" | "NONE" {
  if (!deadline) return "NONE";
  const remaining = deadline - Date.now();
  if (remaining <= 0) return "OVERDUE";
  if (remaining <= 2 * 60 * 60 * 1000) return "DUE_SOON";
  return "ON_TRACK";
}

export async function verifyViolation(violationId: string): Promise<Violation> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/violations/${violationId}/verify`,
    { method: "PATCH" }
  );
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Verification failed: ${response.status} ${body}`);
  }
  return response.json();
}

export function exportViolationsCsv(violations: Violation[]): void {
  const headers = [
    "ID", "Type", "Severity", "Status", "Assigned To",
    "SLA Status", "Latitude", "Longitude", "AI Risk",
    "AI Risk Level", "Created At", "Updated At"
  ];
  const escape = (value: unknown) => `"${String(value ?? "").replaceAll('"', '""')}"`;
  const rows = violations.map(v => [
    v.id, v.violationType, v.severity, v.status, v.assignedTo ?? "",
    v.slaDeadline ? (v.slaDeadline < Date.now() ? "BREACHED" : "ON_TRACK") : "NONE",
    v.latitude ?? "", v.longitude ?? "", v.aiRiskScore ?? "",
    v.aiRiskLevel ?? "", new Date(v.createdAt).toISOString(),
    new Date(v.updatedAt).toISOString()
  ].map(escape).join(","));
  const blob = new Blob([[headers.map(escape).join(","), ...rows].join("\n")], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `minegov-violations-${new Date().toISOString().slice(0,10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
