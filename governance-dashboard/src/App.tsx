import {
  AlertTriangle,
  BarChart3,
  CheckCheck,
  Bell,
  CheckCircle2,
  ChevronDown,
  Download,
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
  exportViolationsCsv,
  getAuditLogs,
  getViolations,
  verifyViolation,
  type AuditLog,
  type Violation,
} from "./api";

import ViolationMap from "./ViolationMap";

const API_BASE_URL = "http://127.0.0.1:8000";

async function assignViolation(violationId: string, assignedTo: string) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/violations/${violationId}/assign?assigned_to=${encodeURIComponent(assignedTo)}`,
    { method: "PATCH" }
  );
  if (!response.ok) throw new Error(`Assignment failed: ${response.status}`);
  return response.json();
}

async function updateViolationStatus(violationId: string, status: string) {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/violations/${violationId}/status?status=${encodeURIComponent(status)}`,
    { method: "PATCH" }
  );
  if (!response.ok) throw new Error(`Status update failed: ${response.status}`);
  return response.json();
}


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

  const [auditLogs, setAuditLogs] =
    useState<AuditLog[]>([]);

  const [auditLoading, setAuditLoading] =
    useState(false);

  const [verifying, setVerifying] = useState(false);
  const [role, setRole] = useState("SUPERVISOR");
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [activeNav, setActiveNav] = useState("Dashboard");
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

  const canAssign = role === "SUPERVISOR" || role === "ADMIN";
  const canUpdateStatus = role === "SUPERVISOR" || role === "ADMIN";
  const canVerify = role === "INSPECTOR" || role === "ADMIN";


  const loadViolations = async () => {

    try {

      setError("");

      const data = await getViolations();

      setViolations(data);

      if (selectedViolation) {

        const updated =
          data.find(
            (v: Violation) => v.id === selectedViolation.id
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


  useEffect(() => {
    if (!selectedViolation?.id) {
      setAuditLogs([]);
      return;
    }

    let cancelled = false;

    const loadAuditLogs = async () => {
      try {
        setAuditLoading(true);
        const logs = await getAuditLogs(selectedViolation.id);
        if (!cancelled) setAuditLogs(logs);
      } catch (err) {
        console.error("Audit log load failed:", err);
        if (!cancelled) setAuditLogs([]);
      } finally {
        if (!cancelled) setAuditLoading(false);
      }
    };

    loadAuditLogs();

    return () => {
      cancelled = true;
    };
  }, [selectedViolation?.id]);


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

  const getSlaState = (v: Violation) => {
    if (!v.slaDeadline) return "NONE";
    const remaining = v.slaDeadline - Date.now();
    if (remaining <= 0) return "BREACHED";
    if (remaining <= 2 * 60 * 60 * 1000) return "WARNING";
    return "ON_TRACK";
  };

  const getRemainingSlaMs = (v: Violation) => {
    if (!v.slaDeadline) return null;
    return Math.max(0, v.slaDeadline - Date.now());
  };

  const breached =
    violations.filter(v => getSlaState(v) === "BREACHED").length;

  const warning =
    violations.filter(v => getSlaState(v) === "WARNING").length;

  const onTrack =
    violations.filter(v => getSlaState(v) === "ON_TRACK").length;

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


  const escalated = violations.filter(v => v.status.toUpperCase() === "ESCALATED").length;
  const highRisk = violations.filter(v =>
    ["HIGH", "CRITICAL"].includes((v.aiRiskLevel ?? "").toUpperCase())
  ).length;
  const notifications = violations.filter(v =>
    getSlaState(v) === "BREACHED" || v.status.toUpperCase() === "ESCALATED"
  );

  const severityChart = ["CRITICAL", "HIGH", "MEDIUM", "LOW"].map(level => ({
    label: level,
    count: violations.filter(v => v.severity.toUpperCase() === level).length,
  }));

  const statusChart = [
    ["SUBMITTED", "Submitted"],
    ["ASSIGNED", "Assigned"],
    ["IN_PROGRESS", "In Progress"],
    ["RESOLVED", "Resolved"],
    ["VERIFIED", "Verified"],
    ["CLOSED", "Closed"],
    ["ESCALATED", "Escalated"],
  ].map(([key, label]) => ({
    key,
    label,
    count: violations.filter(v => v.status.toUpperCase() === key).length,
  }));

  const aiRiskChart = ["CRITICAL", "HIGH", "MEDIUM", "LOW"].map(level => ({
    label: level,
    count: violations.filter(v => (v.aiRiskLevel ?? "").toUpperCase() === level).length,
  }));

  const trendChart = useMemo(() => {
    const days = 14;
    const now = new Date();
    const items: { label: string; count: number }[] = [];
    for (let i = days - 1; i >= 0; i -= 1) {
      const date = new Date(now);
      date.setHours(0, 0, 0, 0);
      date.setDate(date.getDate() - i);
      const next = new Date(date);
      next.setDate(next.getDate() + 1);
      const count = violations.filter(v => v.createdAt >= date.getTime() && v.createdAt < next.getTime()).length;
      items.push({
        label: date.toLocaleDateString(undefined, { month: "short", day: "numeric" }),
        count,
      });
    }
    return items;
  }, [violations]);

  const trendMax = Math.max(1, ...trendChart.map(item => item.count));
  const statusTotal = Math.max(1, violations.length);
  const statusSegments = statusChart.filter(item => item.count > 0);
  let statusOffset = 0;
  const statusGradient = statusSegments.length
    ? statusSegments.map((item, index) => {
        const colors = ["#111315", "#36393d", "#686d72", "#969b9f", "#b7bbbe", "#d0d3d5", "#7b8085"];
        const start = statusOffset;
        statusOffset += (item.count / statusTotal) * 100;
        return `${colors[index % colors.length]} ${start}% ${statusOffset}%`;
      }).join(", ")
    : "#e5e7e9 0% 100%";


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

      if (selectedViolation?.id) {
        try {
          setAuditLogs(
            await getAuditLogs(selectedViolation.id)
          );
        } catch (auditErr) {
          console.error(auditErr);
        }
      }

    } catch (err) {

      console.error(err);

      alert(
        "Assignment failed."
      );

    } finally {

      setAssigning(false);
    }
  };


  useEffect(() => {
    const interval = window.setInterval(() => {
      loadViolations();
    }, 30000);
    return () => window.clearInterval(interval);
  }, [selectedViolation?.id]);


  const handleVerify = async () => {
    if (!canVerify || !selectedViolation || selectedViolation.status.toUpperCase() !== "RESOLVED") return;
    try {
      setVerifying(true);
      await verifyViolation(selectedViolation.id);
      await loadViolations();
      setAuditLogs(await getAuditLogs(selectedViolation.id));
    } catch (err) {
      console.error(err);
      alert("Verification failed. Only RESOLVED violations can be verified.");
    } finally {
      setVerifying(false);
    }
  };

  const handleExport = () => {
    exportViolationsCsv(filteredViolations);
  };

  const roleLabel =
    role === "ADMIN"
      ? "Administrator"
      : role === "INSPECTOR"
        ? "Inspector"
        : "Supervisor";

  const roleInitials =
    role === "ADMIN"
      ? "AD"
      : role === "INSPECTOR"
        ? "IN"
        : "SU";

  const handleNav = (label: string) => {
    setActiveNav(label);
    setMobileNavOpen(false);

    if (label === "Reports") {
      handleExport();
      return;
    }

    if (label === "Audit Trail") {
      const violation = selectedViolation ?? violations[0];
      if (violation) {
        openDetails(violation);
        window.setTimeout(() => {
          document
            .getElementById("audit-trail-section")
            ?.scrollIntoView({ behavior: "smooth", block: "start" });
        }, 80);
      }
      return;
    }

    const targets: Record<string, string> = {
      Dashboard: "dashboard-home",
      Violations: "violations-section",
      "GIS Map": "gis-section",
      Assignments: "violations-section",
      "SLA Monitoring": "governance-summary",
      Analytics: "analytics-section",
      Settings: "settings-anchor",
    };

    if (label === "Assignments") {
      setStatusFilter("ASSIGNED");
    }

    const targetId = targets[label];
    if (!targetId) return;

    requestAnimationFrame(() => {
      document.getElementById(targetId)?.scrollIntoView({
        behavior: "smooth",
        block: "start",
      });
    });
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

        if (selectedViolation?.id) {
          try {
            setAuditLogs(
              await getAuditLogs(selectedViolation.id)
            );
          } catch (auditErr) {
            console.error(auditErr);
          }
        }

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

      <aside className={`sidebar ${mobileNavOpen ? "mobile-open" : ""}`}>

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
          <div className="nav-section">OVERVIEW</div>

          {[
            ["Dashboard", LayoutDashboard],
            ["Violations", ClipboardList],
            ["GIS Map", Map],
          ].map(([label, Icon]) => (
            <button
              key={label as string}
              type="button"
              className={`nav-item ${activeNav === label ? "active" : ""}`}
              onClick={() => handleNav(label as string)}
            >
              <Icon size={18} />
              <span>{label as string}</span>
            </button>
          ))}

          <div className="nav-section">GOVERNANCE</div>

          {[
            ["Assignments", Users],
            ["SLA Monitoring", Clock3],
            ["Analytics", BarChart3],
            ["Reports", FileText],
          ].map(([label, Icon]) => (
            <button
              key={label as string}
              type="button"
              className={`nav-item ${activeNav === label ? "active" : ""}`}
              onClick={() => handleNav(label as string)}
            >
              <Icon size={18} />
              <span>{label as string}</span>
            </button>
          ))}

          <div className="nav-section">SYSTEM</div>

          {[
            ["Audit Trail", ShieldCheck],
            ["Settings", Settings],
          ].map(([label, Icon]) => (
            <button
              key={label as string}
              type="button"
              className={`nav-item ${activeNav === label ? "active" : ""}`}
              onClick={() => handleNav(label as string)}
            >
              <Icon size={18} />
              <span>{label as string}</span>
            </button>
          ))}
        </nav>


        <div className="sidebar-footer">

          <div className="system-status">

            <span className="status-dot" />

            System Online

          </div>

        </div>

      </aside>


      {/* MAIN */}

      {notificationsOpen && (
        <div className="notification-popover">
          <strong>Governance Alerts</strong>
          {notifications.length === 0 ? (
            <p>No active SLA alerts.</p>
          ) : notifications.slice(0, 6).map(v => (
            <button key={v.id} onClick={() => { openDetails(v); setNotificationsOpen(false); }}>
              <span>{v.id}</span>
              <small>{v.status === "ESCALATED" ? "Escalated" : "SLA Breached"}</small>
            </button>
          ))}
        </div>
      )}

      <main className="main-content">

        <header className="topbar">

          <div className="topbar-left">

            <button
              className="mobile-menu"
              type="button"
              onClick={() => setMobileNavOpen(v => !v)}
              aria-label="Open navigation"
            >
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
            <div className="role-switcher" id="settings-anchor">
              <span>ROLE</span>
              <select value={role} onChange={e => setRole(e.target.value)}>
                <option value="INSPECTOR">Inspector</option>
                <option value="SUPERVISOR">Supervisor</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>

            <button
              className="icon-button notification-button"
              type="button"
              onClick={() => setNotificationsOpen(v => !v)}
              title="Governance alerts"
            >
              <Bell size={18} />
              {notifications.length > 0 && (
                <span className="notification-count">{notifications.length}</span>
              )}
            </button>

            <button
              className="secondary-button compact"
              type="button"
              onClick={handleExport}
            >
              <Download size={15} />
              <span>Export CSV</span>
            </button>

            <button
              className="icon-button"
              type="button"
              onClick={loadViolations}
              title="Refresh"
            >
              <RefreshCw size={18} />
            </button>

            <div className="user-profile">
              <div className="avatar">{roleInitials}</div>
              <div className="user-info">
                <strong>{roleLabel}</strong>
                <span>Government</span>
              </div>
              <ChevronDown size={16} />
            </div>
          </div>

        </header>


        <div className="dashboard-content" id="dashboard-home">

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

          <section className="panel" id="violations-section">

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

                  {role === "ADMIN" && (
                    <>
                      <option value="VERIFIED">Verified</option>
                      <option value="CLOSED">Closed</option>
                    </>
                  )}

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
                                  getSlaState(violation)
                                )}
                              >
                                {getSlaState(violation).replace(
                                  "_",
                                  " "
                                )}
                              </span>

                              {getRemainingSlaMs(violation) !==
                                null && (

                                <small>
                                  {formatSla(
                                    getRemainingSlaMs(violation)
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


          {/* ANALYTICS */}

          <section className="analytics-grid" id="analytics-section">

            <div className="panel chart-panel">
              <div className="panel-header">
                <div>
                  <h2>Severity Distribution</h2>
                  <p>Current violation profile</p>
                </div>
                <BarChart3 size={20} />
              </div>

              <div className="severity-chart">
                <div className="chart-y-axis">
                  <span>{Math.max(5, Math.ceil(Math.max(...severityChart.map(item => item.count), 0) / 5) * 5)}</span>
                  <span>{Math.max(1, Math.ceil(Math.max(...severityChart.map(item => item.count), 0) / 2))}</span>
                  <span>0</span>
                </div>
                <div className="bar-chart-area">
                  <div className="chart-grid-lines"><i /><i /><i /></div>
                  <div className="bar-chart-bars">
                    {severityChart.map(item => {
                      const height = item.count > 0 ? Math.max(8, (item.count / Math.max(1, ...severityChart.map(x => x.count))) * 100) : 4;
                      return (
                        <div className="bar-column" key={item.label}>
                          <div className="bar-value">{item.count > 0 ? `${Math.round((item.count / total) * 100)}%` : "0%"}</div>
                          <div className="bar-fill" style={{ height: `${height}%` }} />
                          <strong>{item.count}</strong>
                          <span>{item.label.charAt(0) + item.label.slice(1).toLowerCase()}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>

            <div className="panel chart-panel">
              <div className="panel-header">
                <div>
                  <h2>Violation Status</h2>
                  <p>Current workflow status</p>
                </div>
                <Clock3 size={20} />
              </div>

              <div className="status-chart-layout">
                <div className="status-donut" style={{ background: `conic-gradient(${statusGradient})` }}>
                  <div className="status-donut-hole">
                    <strong>{violations.length}</strong>
                    <span>Total</span>
                  </div>
                </div>
                <div className="status-legend">
                  {statusChart.map(item => (
                    <div className="legend-row" key={item.key}>
                      <span className={`legend-dot legend-${item.key.toLowerCase()}`} />
                      <span>{item.label}</span>
                      <strong>{item.count}</strong>
                      <small>{Math.round((item.count / statusTotal) * 100)}%</small>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="panel chart-panel map-panel">
              <div className="panel-header">
                <div>
                  <h2>GIS Locations</h2>
                  <p>Geo-tagged inspection records</p>
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

          <section className="analytics-lower-grid">
            <div className="panel chart-panel trend-panel">
              <div className="panel-header">
                <div>
                  <h2>Violations Trend</h2>
                  <p>Daily submissions · last 14 days</p>
                </div>
                <BarChart3 size={20} />
              </div>
              <div className="trend-chart">
                <div className="trend-y-axis"><span>{trendMax}</span><span>{Math.round(trendMax / 2)}</span><span>0</span></div>
                <div className="trend-plot">
                  <div className="trend-grid-lines"><i /><i /><i /></div>
                  <div className="trend-bars">
                    {trendChart.map((item, index) => (
                      <div className="trend-column" key={item.label}>
                        <div className="trend-bar" style={{ height: `${Math.max(item.count ? 6 : 2, (item.count / trendMax) * 100)}%` }} title={`${item.label}: ${item.count}`} />
                        {(index % 2 === 0 || index === trendChart.length - 1) && <span>{item.label}</span>}
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            </div>

            <div className="panel chart-panel ai-risk-panel">
              <div className="panel-header">
                <div>
                  <h2>AI Risk Distribution</h2>
                  <p>AI analysis risk levels</p>
                </div>
                <ScanSearch size={20} />
              </div>
              <div className="ai-risk-chart">
                {aiRiskChart.map(item => {
                  const percentage = total > 0 ? Math.round((item.count / total) * 100) : 0;
                  return (
                    <div className="ai-risk-row" key={item.label}>
                      <span>{item.label}</span>
                      <strong>{item.count}</strong>
                      <small>{percentage}%</small>
                      <div className="ai-risk-track"><div style={{ width: `${percentage}%` }} /></div>
                    </div>
                  );
                })}
              </div>
            </div>
          </section>

          {/* GOVERNANCE */}

          <section className="governance-summary" id="governance-summary">

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

              <div className="analytics-strip">
                <div><span>Escalated</span><strong>{escalated}</strong></div>
                <div><span>High / Critical AI Risk</span><strong>{highRisk}</strong></div>
                <div><span>SLA Compliance</span><strong>{slaCompliance}%</strong></div>
                <div><span>Open</span><strong>{open}</strong></div>
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
                    selectedViolation.localId ?? "—"
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

                  <EvidenceItem
                    label="Document"
                    available={
                      !!selectedViolation.documentUri
                    }
                  />

                </div>

                {selectedViolation.documentUri && (
                  <div className="document-preview">
                    <a
                      href={`http://127.0.0.1:8000${selectedViolation.documentUri}`}
                      target="_blank"
                      rel="noreferrer"
                      className="primary-button"
                    >
                      Open Document
                    </a>
                  </div>
                )}

                {selectedViolation.ocrText && (
                  <div className="ocr-detail-box">
                    <span>OCR TEXT</span>
                    <p>{selectedViolation.ocrText}</p>
                  </div>
                )}

                {selectedViolation.voiceTranscript && (
                  <div className="voice-transcript-box">
                    <div className="voice-transcript-header">
                      <span>WHISPER TRANSCRIPT</span>

                      {selectedViolation.voiceLanguage && (
                        <span className="voice-language">
                          {selectedViolation.voiceLanguage.toUpperCase()}
                        </span>
                      )}
                    </div>

                    <p>{selectedViolation.voiceTranscript}</p>
                  </div>
                )}

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
                        (selectedViolation.aiConfidence ?? 0) *
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
                          selectedViolation.aiRiskLevel ?? null
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


              {/* AUDIT TRAIL */}

              <div className="detail-section audit-section" id="audit-trail-section">

                <div className="audit-section-header">
                  <div>
                    <h3>
                      Audit Trail
                    </h3>

                    <p className="audit-subtitle">
                      Tamper-evident governance history
                    </p>
                  </div>

                  <ShieldCheck size={19} />
                </div>

                {auditLoading ? (
                  <div className="audit-empty">
                    Loading audit history...
                  </div>
                ) : auditLogs.length === 0 ? (
                  <div className="audit-empty">
                    No audit records found.
                  </div>
                ) : (
                  <div className="audit-timeline">
                    {auditLogs.map((log, index) => (
                      <div
                        className="audit-item"
                        key={log.id}
                      >
                        <div className="audit-rail">
                          <span className="audit-dot" />
                          {index < auditLogs.length - 1 && (
                            <span className="audit-line" />
                          )}
                        </div>

                        <div className="audit-card">
                          <div className="audit-top">
                            <strong>
                              {log.action.replaceAll("_", " ")}
                            </strong>

                            <span>
                              {formatDate(log.timestamp)}
                            </span>
                          </div>

                          <div className="audit-meta">
                            Actor:{" "}
                            <b>
                              {log.actor || "SYSTEM"}
                            </b>
                          </div>

                          {(log.oldStatus || log.newStatus) && (
                            <div className="audit-status-flow">
                              <span>
                                {log.oldStatus || "—"}
                              </span>

                              <span className="audit-arrow">
                                →
                              </span>

                              <span>
                                {log.newStatus || "—"}
                              </span>
                            </div>
                          )}

                          {log.details && (
                            <div className="audit-details">
                              {log.details}
                            </div>
                          )}

                          {log.currentHash && (
                            <div className="audit-hash">
                              <span>HASH</span>
                              <code>
                                {log.currentHash.slice(0, 24)}...
                              </code>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
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
                        getSlaState(selectedViolation)
                      }
                    </strong>

                  </div>


                  <div>

                    <span>
                      Remaining
                    </span>

                    <strong>
                      {formatSla(
                        getRemainingSlaMs(selectedViolation)
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
                      disabled={!canAssign || assigning}
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
                      !canAssign ||
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
                  Resolution Verification
                </h3>

                <button
                  className="verify-button"
                  disabled={!canVerify || verifying || selectedViolation.status.toUpperCase() !== "RESOLVED"}
                  onClick={handleVerify}
                >
                  <CheckCheck size={16} />
                  {verifying ? "Verifying..." : "Verify Resolution"}
                </button>

                <p className="helper-text">
                  {canVerify
                    ? "Inspector verification is required after a violation is marked RESOLVED."
                    : "Switch to Inspector role to verify a resolved violation."}
                </p>

              </div>

              <div className="detail-section">

                <h3>
                  Update Status
                </h3>

                <select
                  className="status-select"
                  value={
                    selectedViolation.status
                  }
                  disabled={!canUpdateStatus || updatingStatus}
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

                  {role === "ADMIN" && (
                    <>
                      <option value="VERIFIED">Verified</option>
                      <option value="CLOSED">Closed</option>
                    </>
                  )}

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

      <div className="kpi-content">

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
