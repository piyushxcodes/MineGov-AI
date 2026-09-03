import {
  AlertTriangle,
  BarChart3,
  Bell,
  CheckCircle2,
  ChevronDown,
  ClipboardList,
  Clock3,
  FileText,
  LayoutDashboard,
  Map,
  Menu,
  RefreshCw,
  Search,
  Settings,
  ShieldCheck,
  UserPlus,
  Users,
  X,
  ScanSearch,
} from "lucide-react";

import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  assignViolation,
  getViolations,
  updateViolationStatus,
  type Violation,
} from "./api";

import ViolationMap from "./ViolationMap";


function formatDate(timestamp: number) {
  return new Date(timestamp).toLocaleString();
}


function formatSla(ms: number | null) {
  if (ms === null) return "Not assigned";

  const totalMinutes = Math.floor(ms / 60000);

  const days = Math.floor(totalMinutes / 1440);
  const hours = Math.floor((totalMinutes % 1440) / 60);
  const minutes = totalMinutes % 60;

  if (days > 0) return `${days}d ${hours}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;

  return `${minutes}m`;
}


function severityClass(severity: string) {

  switch (severity.toUpperCase()) {

    case "CRITICAL":
      return "badge badge-critical";

    case "HIGH":
      return "badge badge-high";

    case "MEDIUM":
      return "badge badge-medium";

    default:
      return "badge badge-low";
  }
}


function statusClass(status: string) {

  switch (status.toUpperCase()) {

    case "CLOSED":
    case "VERIFIED":
    case "RESOLVED":
      return "badge badge-success";

    case "ASSIGNED":
    case "IN_PROGRESS":
      return "badge badge-info";

    default:
      return "badge badge-neutral";
  }
}


function slaClass(status: string) {

  switch (status) {

    case "BREACHED":
      return "badge badge-critical";

    case "WARNING":
      return "badge badge-warning";

    case "ON_TRACK":
      return "badge badge-success";

    default:
      return "badge badge-neutral";
  }
}


function aiRiskClass(level: string | null) {

  switch ((level ?? "").toUpperCase()) {

    case "CRITICAL":
      return "badge badge-critical";

    case "HIGH":
      return "badge badge-high";

    case "MEDIUM":
      return "badge badge-medium";

    case "LOW":
      return "badge badge-low";

    default:
      return "badge badge-neutral";
  }
}


function App() {

  const [violations, setViolations] =
    useState<Violation[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [search, setSearch] =
    useState("");

  const [severityFilter, setSeverityFilter] =
    useState("ALL");

  const [statusFilter, setStatusFilter] =
    useState("ALL");

  const [selectedViolation, setSelectedViolation] =
    useState<Violation | null>(null);

  const [assigning, setAssigning] =
    useState(false);

  const [assignedTo, setAssignedTo] =
    useState("");

  const [updatingStatus, setUpdatingStatus] =
    useState(false);


  const loadViolations = async () => {

    try {

      setError("");

      const data = await getViolations();

      setViolations(data.violations);

      if (selectedViolation) {

        const updated =
          data.violations.find(
            v => v.id === selectedViolation.id
          );

        if (updated) {

          setSelectedViolation(updated);

          setAssignedTo(
            updated.assignedTo ?? ""
          );
        }
      }

    } catch (err) {

      console.error(err);

      setError(
        "Unable to load violations. Make sure FastAPI is running."
      );

    } finally {

      setLoading(false);
    }
  };


  useEffect(() => {

    loadViolations();

    const interval =
      setInterval(() => {
        loadViolations();
      }, 15000);

    return () =>
      clearInterval(interval);

  }, []);


  const filteredViolations =
    useMemo(() => {

      return violations.filter(v => {

        const searchableText =
          `${v.id} ${v.localId} ${v.violationType} ${v.description}`
            .toLowerCase();

        const matchesSearch =
          searchableText.includes(
            search.toLowerCase()
          );

        const matchesSeverity =
          severityFilter === "ALL" ||
          v.severity.toUpperCase() ===
            severityFilter;

        const matchesStatus =
          statusFilter === "ALL" ||
          v.status.toUpperCase() ===
            statusFilter;

        return (
          matchesSearch &&
          matchesSeverity &&
          matchesStatus
        );
      });

    }, [
      violations,
      search,
      severityFilter,
      statusFilter,
    ]);


  const total =
    violations.length;

  const open =
    violations.filter(
      v =>
        ![
          "CLOSED",
          "VERIFIED",
        ].includes(
          v.status.toUpperCase()
        )
    ).length;

  const critical =
    violations.filter(
      v =>
        v.severity.toUpperCase() ===
        "CRITICAL"
    ).length;

  const assigned =
    violations.filter(
      v => !!v.assignedTo
    ).length;

  const inProgress =
    violations.filter(
      v =>
        v.status.toUpperCase() ===
        "IN_PROGRESS"
    ).length;

  const resolved =
    violations.filter(
      v =>
        [
          "RESOLVED",
          "VERIFIED",
          "CLOSED",
        ].includes(
          v.status.toUpperCase()
        )
    ).length;

  const breached =
    violations.filter(
      v =>
        v.slaStatus ===
        "BREACHED"
    ).length;

  const warning =
    violations.filter(
      v =>
        v.slaStatus ===
        "WARNING"
    ).length;

  const onTrack =
    violations.filter(
      v =>
        v.slaStatus ===
        "ON_TRACK"
    ).length;

  const geoTagged =
    violations.filter(
      v =>
        v.latitude !== null &&
        v.longitude !== null
    ).length;

  const slaEligible =
    violations.filter(
      v =>
        v.slaDeadline !== null
    ).length;

  const slaCompliance =
    slaEligible > 0
      ? Math.round(
          ((slaEligible - breached) /
            slaEligible) *
            100
        )
      : 100;


  const openDetails = (
    violation: Violation
  ) => {

    setSelectedViolation(
      violation
    );

    setAssignedTo(
      violation.assignedTo ?? ""
    );
  };


  const handleAssign = async () => {

    if (
      !selectedViolation ||
      !assignedTo.trim()
    ) {
      return;
    }

    try {

      setAssigning(true);

      await assignViolation(
        selectedViolation.id,
        assignedTo.trim()
      );

      await loadViolations();

    } catch (err) {

      console.error(err);

      alert(
        "Assignment failed."
      );

    } finally {

      setAssigning(false);
    }
  };


  const handleStatusChange =
    async (status: string) => {

      if (!selectedViolation) {
        return;
      }

      try {

        setUpdatingStatus(true);

        await updateViolationStatus(
          selectedViolation.id,
          status
        );

        await loadViolations();

      } catch (err) {

        console.error(err);

        alert(
          "Status update failed."
        );

      } finally {

        setUpdatingStatus(false);
      }
    };


  return (

    <div className="app-shell">

      {/* SIDEBAR */}

      <aside className="sidebar">

        <div className="brand">

          <div className="brand-mark">
            MG
          </div>

          <div>

            <div className="brand-title">
              MineGov AI
            </div>

            <div className="brand-subtitle">
              Governance Console
            </div>

          </div>

        </div>


        <nav className="sidebar-nav">

          <div className="nav-section">
            OVERVIEW
          </div>

          <div className="nav-item active">
            <LayoutDashboard size={18} />
            Dashboard
          </div>

          <div className="nav-item">
            <ClipboardList size={18} />
            Violations
          </div>

          <div className="nav-item">
            <Map size={18} />
            GIS Map
          </div>


          <div className="nav-section">
            GOVERNANCE
          </div>

          <div className="nav-item">
            <Users size={18} />
            Assignments
          </div>

          <div className="nav-item">
            <Clock3 size={18} />
            SLA Monitoring
          </div>

          <div className="nav-item">
            <BarChart3 size={18} />
            Analytics
          </div>

          <div className="nav-item">
            <FileText size={18} />
            Reports
          </div>


          <div className="nav-section">
            SYSTEM
          </div>

          <div className="nav-item">
            <ShieldCheck size={18} />
            Audit Trail
          </div>

          <div className="nav-item">
            <Settings size={18} />
            Settings
          </div>

        </nav>


        <div className="sidebar-footer">

          <div className="system-status">

            <span className="status-dot" />

            System Online

          </div>

        </div>

      </aside>


      {/* MAIN */}

      <main className="main-content">

        <header className="topbar">

          <div className="topbar-left">

            <button className="mobile-menu">
              <Menu size={20} />
            </button>

            <div>

              <div className="page-title">
                Governance Dashboard
              </div>

              <div className="page-subtitle">
                Mining inspection & compliance monitoring
              </div>

            </div>

          </div>


          <div className="topbar-actions">

            <button
              className="icon-button"
              onClick={loadViolations}
              title="Refresh"
            >
              <RefreshCw size={18} />
            </button>

            <button className="icon-button">
              <Bell size={18} />
            </button>


            <div className="user-profile">

              <div className="avatar">
                AD
              </div>

              <div className="user-info">

                <strong>
                  Administrator
                </strong>

                <span>
                  Government
                </span>

              </div>

              <ChevronDown size={16} />

            </div>

          </div>

        </header>


        <div className="dashboard-content">

          {error && (

            <div className="error-banner">

              <AlertTriangle size={18} />

              {error}

            </div>

          )}


          {/* KPI */}

          <section className="kpi-grid">

            <Kpi
              icon={<ClipboardList size={20} />}
              label="Total Violations"
              value={total}
            />

            <Kpi
              icon={<Clock3 size={20} />}
              label="Open Violations"
              value={open}
            />

            <Kpi
              icon={<AlertTriangle size={20} />}
              label="Critical"
              value={critical}
            />

            <Kpi
              icon={<Users size={20} />}
              label="Assigned"
              value={assigned}
            />

            <Kpi
              icon={<ShieldCheck size={20} />}
              label="SLA Compliance"
              value={`${slaCompliance}%`}
            />

            <Kpi
              icon={<Map size={20} />}
              label="Geo Tagged"
              value={geoTagged}
            />

          </section>


          {/* VIOLATIONS */}

          <section className="panel">

            <div className="panel-header">

              <div>

                <h2>
                  Violations
                </h2>

                <p>
                  {filteredViolations.length} records
                </p>

              </div>


              <div className="table-controls">

                <div className="search-box">

                  <Search size={17} />

                  <input
                    placeholder="Search violations..."
                    value={search}
                    onChange={e =>
                      setSearch(e.target.value)
                    }
                  />

                </div>


                <select
                  value={severityFilter}
                  onChange={e =>
                    setSeverityFilter(
                      e.target.value
                    )
                  }
                >

                  <option value="ALL">
                    All Severities
                  </option>

                  <option value="CRITICAL">
                    Critical
                  </option>

                  <option value="HIGH">
                    High
                  </option>

                  <option value="MEDIUM">
                    Medium
                  </option>

                  <option value="LOW">
                    Low
                  </option>

                </select>


                <select
                  value={statusFilter}
                  onChange={e =>
                    setStatusFilter(
                      e.target.value
                    )
                  }
                >

                  <option value="ALL">
                    All Status
                  </option>

                  <option value="SUBMITTED">
                    Submitted
                  </option>

                  <option value="ASSIGNED">
                    Assigned
                  </option>

                  <option value="IN_PROGRESS">
                    In Progress
                  </option>

                  <option value="RESOLVED">
                    Resolved
                  </option>

                  <option value="VERIFIED">
                    Verified
                  </option>

                  <option value="CLOSED">
                    Closed
                  </option>

                </select>

              </div>

            </div>


            <div className="table-wrapper">

              {loading ? (

                <div className="empty-state">
                  Loading violations...
                </div>

              ) : filteredViolations.length === 0 ? (

                <div className="empty-state">
                  No violations found.
                </div>

              ) : (

                <table>

                  <thead>

                    <tr>

                      <th>Violation</th>
                      <th>Type</th>
                      <th>Severity</th>
                      <th>Status</th>
                      <th>Assignment</th>
                      <th>SLA</th>
                      <th>AI Risk</th>
                      <th>Vision</th>
                      <th>Created</th>

                    </tr>

                  </thead>


                  <tbody>

                    {filteredViolations.map(
                      violation => (

                        <tr
                          key={violation.id}
                          className="clickable-row"
                          onClick={() =>
                            openDetails(
                              violation
                            )
                          }
                        >

                          <td>

                            <strong>
                              {violation.id}
                            </strong>

                            <span className="table-subtext">
                              {violation.localId}
                            </span>

                          </td>


                          <td>
                            {violation.violationType}
                          </td>


                          <td>

                            <span
                              className={severityClass(
                                violation.severity
                              )}
                            >
                              {violation.severity}
                            </span>

                          </td>


                          <td>

                            <span
                              className={statusClass(
                                violation.status
                              )}
                            >
                              {violation.status.replace(
                                "_",
                                " "
                              )}
                            </span>

                          </td>


                          <td>
                            {violation.assignedTo ??
                              "Unassigned"}
                          </td>


                          <td>

                            <div className="sla-cell">

                              <span
                                className={slaClass(
                                  violation.slaStatus
                                )}
                              >
                                {violation.slaStatus.replace(
                                  "_",
                                  " "
                                )}
                              </span>

                              {violation.remainingSlaMs !==
                                null && (

                                <small>
                                  {formatSla(
                                    violation.remainingSlaMs
                                  )}
                                </small>

                              )}

                            </div>

                          </td>


                          <td>

                            {violation.aiRiskScore !==
                            null ? (

                              <div className="ai-table-risk">

                                <strong>
                                  {violation.aiRiskScore}
                                </strong>

                                <span>
                                  {violation.aiRiskLevel ??
                                    "UNKNOWN"}
                                </span>

                              </div>

                            ) : (

                              <span className="muted">
                                Pending
                              </span>

                            )}

                          </td>


                          <td>

                            {violation.aiVisionScore !==
                            null ? (

                              <div className="ai-table-risk">

                                <strong>
                                  {violation.aiVisionScore}
                                </strong>

                                <span>
                                  AI Vision
                                </span>

                              </div>

                            ) : (

                              <span className="muted">
                                —
                              </span>

                            )}

                          </td>


                          <td>
                            {formatDate(
                              violation.createdAt
                            )}
                          </td>

                        </tr>

                      )
                    )}

                  </tbody>

                </table>

              )}

            </div>

          </section>


          {/* BOTTOM */}

          <section className="bottom-grid">


            {/* DISTRIBUTION */}

            <div className="panel">

              <div className="panel-header">

                <div>

                  <h2>
                    Severity Distribution
                  </h2>

                  <p>
                    Current violation profile
                  </p>

                </div>

              </div>


              <div className="distribution-list">

                {[
                  "CRITICAL",
                  "HIGH",
                  "MEDIUM",
                  "LOW",
                ].map(severity => {

                  const count =
                    violations.filter(
                      v =>
                        v.severity.toUpperCase() ===
                        severity
                    ).length;

                  const percentage =
                    total > 0
                      ? Math.round(
                          (count / total) *
                            100
                        )
                      : 0;

                  return (

                    <div
                      className="distribution-row"
                      key={severity}
                    >

                      <div>

                        <span
                          className={severityClass(
                            severity
                          )}
                        >
                          {severity}
                        </span>

                        <strong>
                          {count}
                        </strong>

                      </div>


                      <div className="distribution-bar">

                        <div
                          style={{
                            width:
                              `${percentage}%`,
                          }}
                        />

                      </div>

                      <span>
                        {percentage}%
                      </span>

                    </div>

                  );

                })}

              </div>

            </div>


            {/* MAP */}

            <div className="panel">

              <div className="panel-header">

                <div>

                  <h2>
                    GIS Locations
                  </h2>

                  <p>
                    Geo-tagged inspection records
                  </p>

                </div>

                <Map size={20} />

              </div>


              <div className="map-container">

                <ViolationMap
                  violations={violations}
                  onSelect={openDetails}
                />

              </div>

            </div>

          </section>


          {/* GOVERNANCE */}

          <section className="governance-summary">

            <div className="panel">

              <div className="panel-header">

                <div>

                  <h2>
                    Governance Summary
                  </h2>

                  <p>
                    Current compliance workflow
                  </p>

                </div>

                <BarChart3 size={20} />

              </div>


              <div className="governance-grid">

                <SummaryItem
                  label="Submitted"
                  value={
                    violations.filter(
                      v =>
                        v.status ===
                        "SUBMITTED"
                    ).length
                  }
                />

                <SummaryItem
                  label="Assigned"
                  value={assigned}
                />

                <SummaryItem
                  label="In Progress"
                  value={inProgress}
                />

                <SummaryItem
                  label="Resolved"
                  value={resolved}
                />

                <SummaryItem
                  label="SLA Warning"
                  value={warning}
                />

                <SummaryItem
                  label="SLA Breached"
                  value={breached}
                />

                <SummaryItem
                  label="On Track"
                  value={onTrack}
                />

                <SummaryItem
                  label="Geo Tagged"
                  value={geoTagged}
                />

              </div>

            </div>

          </section>

        </div>

      </main>


      {/* DETAIL DRAWER */}

      {selectedViolation && (

        <div className="drawer-backdrop">

          <aside className="violation-drawer">


            <div className="drawer-header">

              <div>

                <span className="drawer-label">
                  VIOLATION DETAILS
                </span>

                <h2>
                  {selectedViolation.id}
                </h2>

              </div>


              <button
                className="icon-button"
                onClick={() =>
                  setSelectedViolation(null)
                }
              >
                <X size={20} />
              </button>

            </div>


            <div className="drawer-content">


              <div className="detail-status-row">

                <span
                  className={severityClass(
                    selectedViolation.severity
                  )}
                >
                  {selectedViolation.severity}
                </span>

                <span
                  className={statusClass(
                    selectedViolation.status
                  )}
                >
                  {selectedViolation.status.replace(
                    "_",
                    " "
                  )}
                </span>

              </div>


              {/* INFORMATION */}

              <div className="detail-section">

                <h3>
                  Violation Information
                </h3>

                <DetailRow
                  label="Type"
                  value={
                    selectedViolation.violationType
                  }
                />

                <DetailRow
                  label="Local ID"
                  value={
                    selectedViolation.localId
                  }
                />

                <DetailRow
                  label="Created"
                  value={formatDate(
                    selectedViolation.createdAt
                  )}
                />

              </div>


              {/* DESCRIPTION */}

              <div className="detail-section">

                <h3>
                  Description
                </h3>

                <p className="detail-description">
                  {selectedViolation.description ||
                    "No description provided."}
                </p>

              </div>


              {/* CONDITION */}

              <div className="detail-section">

                <h3>
                  Observed Condition
                </h3>

                <p className="detail-description">
                  {selectedViolation.observedCondition ||
                    "No observed condition provided."}
                </p>

              </div>


              {/* LOCATION */}

              <div className="detail-section">

                <h3>
                  Location
                </h3>

                {selectedViolation.latitude !==
                    null &&
                selectedViolation.longitude !==
                    null ? (

                  <div className="location-box">

                    <Map size={18} />

                    <span>
                      {
                        selectedViolation.latitude
                      }
                      ,{" "}
                      {
                        selectedViolation.longitude
                      }
                    </span>

                  </div>

                ) : (

                  <p className="muted">
                    GPS location unavailable.
                  </p>

                )}

              </div>


              {/* EVIDENCE */}

              <div className="detail-section">

                <h3>
                  Evidence
                </h3>

                <div className="evidence-grid">

                  <EvidenceItem
                    label="Photo"
                    available={
                      !!selectedViolation.photoUri
                    }
                  />

                  <EvidenceItem
                    label="Video"
                    available={
                      !!selectedViolation.videoUri
                    }
                  />

                  <EvidenceItem
                    label="Voice"
                    available={
                      !!selectedViolation.voiceUri
                    }
                  />

                </div>

              </div>


              {/* EXISTING AI */}

              <div className="detail-section">

                <h3>
                  AI Risk Analysis
                </h3>


                <div className="ai-risk-card">

                  <div className="ai-risk-score">

                    <span>
                      Risk Score
                    </span>

                    <strong>
                      {selectedViolation.aiRiskScore !==
                      null
                        ? `${selectedViolation.aiRiskScore}/100`
                        : "Not analyzed"}
                    </strong>

                  </div>


                  <div className="ai-risk-level">

                    <span>
                      Risk Level
                    </span>

                    <strong>
                      {selectedViolation.aiRiskLevel ??
                        "Pending"}
                    </strong>

                  </div>

                </div>


                <div className="ai-finding">

                  <span>
                    AI Finding
                  </span>

                  <p>
                    {selectedViolation.aiFinding ??
                      "AI analysis has not been performed yet."}
                  </p>

                </div>


                {selectedViolation.aiConfidence !==
                  null && (

                  <div className="ai-confidence">

                    <span>
                      AI Confidence
                    </span>

                    <strong>
                      {Math.round(
                        selectedViolation.aiConfidence *
                          100
                      )}
                      %
                    </strong>

                  </div>

                )}

              </div>


              {/* NEW YOLO VISION */}

              <div className="detail-section">

                <div className="vision-heading">

                  <div>

                    <h3>
                      AI Vision Detection
                    </h3>

                    <p className="vision-subtitle">
                      YOLO PPE & hazard analysis
                    </p>

                  </div>

                  <ScanSearch size={20} />

                </div>


                {selectedViolation.aiVisionScore !==
                  null ? (

                  <>

                    <div className="vision-score-card">

                      <div>

                        <span>
                          Vision Risk Score
                        </span>

                        <strong>
                          {
                            selectedViolation.aiVisionScore
                          }
                          /100
                        </strong>

                      </div>

                      <span
                        className={aiRiskClass(
                          selectedViolation.aiRiskLevel
                        )}
                      >
                        {selectedViolation.aiRiskLevel ??
                          "UNKNOWN"}
                      </span>

                    </div>


                    <div className="vision-findings">

                      <div className="vision-findings-header">

                        <strong>
                          Detected Conditions
                        </strong>

                        <span>
                          {
                            selectedViolation.aiDetections
                              ?.length ?? 0
                          }{" "}
                          objects
                        </span>

                      </div>


                      {selectedViolation.aiDetections &&
                      selectedViolation.aiDetections.length >
                        0 ? (

                        <div className="detection-list">

                          {selectedViolation.aiDetections.map(
                            (
                              detection,
                              index
                            ) => (

                              <div
                                className="detection-item"
                                key={`${detection.class}-${index}`}
                              >

                                <div className="detection-icon">
                                  <ScanSearch
                                    size={16}
                                  />
                                </div>


                                <div className="detection-info">

                                  <strong>
                                    {
                                      detection.class
                                    }
                                  </strong>

                                  <span>
                                    Bounding box detected
                                  </span>

                                </div>


                                <div className="detection-confidence">

                                  {Math.round(
                                    detection.confidence *
                                      100
                                  )}
                                  %

                                </div>

                              </div>

                            )
                          )}

                        </div>

                      ) : (

                        <div className="no-detection">

                          <CheckCircle2
                            size={18}
                          />

                          <span>
                            No PPE safety violation
                            detected.
                          </span>

                        </div>

                      )}

                    </div>


                    {selectedViolation.aiVisionFindings && (

                      <div className="vision-finding-box">

                        <span>
                          AI Vision Finding
                        </span>

                        <p>
                          {
                            selectedViolation.aiVisionFindings
                          }
                        </p>

                      </div>

                    )}

                  </>

                ) : (

                  <div className="vision-pending">

                    <ScanSearch
                      size={20}
                    />

                    <div>

                      <strong>
                        Vision analysis pending
                      </strong>

                      <span>
                        No YOLO image analysis has
                        been stored for this violation.
                      </span>

                    </div>

                  </div>

                )}

              </div>


              {/* SLA */}

              <div className="detail-section">

                <h3>
                  SLA Monitoring
                </h3>

                <div className="sla-detail">

                  <div>

                    <span>
                      SLA Status
                    </span>

                    <strong>
                      {
                        selectedViolation.slaStatus
                      }
                    </strong>

                  </div>


                  <div>

                    <span>
                      Remaining
                    </span>

                    <strong>
                      {formatSla(
                        selectedViolation.remainingSlaMs
                      )}
                    </strong>

                  </div>

                </div>

              </div>


              {/* ASSIGNMENT */}

              <div className="detail-section">

                <h3>
                  Assignment
                </h3>


                <div className="assignment-box">

                  <div className="assignment-input">

                    <UserPlus size={17} />

                    <input
                      placeholder="Inspector / officer name"
                      value={assignedTo}
                      onChange={e =>
                        setAssignedTo(
                          e.target.value
                        )
                      }
                    />

                  </div>


                  <button
                    className="primary-button"
                    disabled={
                      assigning ||
                      !assignedTo.trim()
                    }
                    onClick={handleAssign}
                  >
                    {assigning
                      ? "Assigning..."
                      : "Assign Violation"}
                  </button>

                </div>


                {selectedViolation.assignedTo && (

                  <p className="assigned-text">

                    Currently assigned to{" "}

                    <strong>
                      {
                        selectedViolation.assignedTo
                      }
                    </strong>

                  </p>

                )}

              </div>


              {/* STATUS */}

              <div className="detail-section">

                <h3>
                  Update Status
                </h3>

                <select
                  className="status-select"
                  value={
                    selectedViolation.status
                  }
                  disabled={updatingStatus}
                  onChange={e =>
                    handleStatusChange(
                      e.target.value
                    )
                  }
                >

                  <option value="SUBMITTED">
                    Submitted
                  </option>

                  <option value="ASSIGNED">
                    Assigned
                  </option>

                  <option value="IN_PROGRESS">
                    In Progress
                  </option>

                  <option value="RESOLVED">
                    Resolved
                  </option>

                  <option value="VERIFIED">
                    Verified
                  </option>

                  <option value="CLOSED">
                    Closed
                  </option>

                </select>

              </div>


            </div>

          </aside>

        </div>

      )}

    </div>
  );
}


/* ============================================================
   SMALL COMPONENTS
============================================================ */


function Kpi({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string | number;
}) {

  return (

    <div className="kpi-card">

      <div className="kpi-icon">
        {icon}
      </div>

      <div>

        <span>
          {label}
        </span>

        <strong>
          {value}
        </strong>

      </div>

    </div>
  );
}


function DetailRow({
  label,
  value,
}: {
  label: string;
  value: string;
}) {

  return (

    <div className="detail-row">

      <span>
        {label}
      </span>

      <strong>
        {value}
      </strong>

    </div>
  );
}


function EvidenceItem({
  label,
  available,
}: {
  label: string;
  available: boolean;
}) {

  return (

    <div className="evidence-item">

      {available ? (
        <CheckCircle2 size={18} />
      ) : (
        <FileText size={18} />
      )}

      <div>

        <strong>
          {label}
        </strong>

        <span>
          {available
            ? "Available"
            : "Not attached"}
        </span>

      </div>

    </div>
  );
}


function SummaryItem({
  label,
  value,
}: {
  label: string;
  value: number;
}) {

  return (

    <div className="summary-item">

      <span>
        {label}
      </span>

      <strong>
        {value}
      </strong>

    </div>
  );
}


export default App;