import {
  MapContainer,
  TileLayer,
  Marker,
  Popup,
  useMap,
} from "react-leaflet";

import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { useEffect } from "react";
import type { Violation } from "./api";

const markerIcon = L.divIcon({
  className: "custom-map-marker",
  html: `<div class="map-marker-dot"></div>`,
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

function MapController({
  violations,
}: {
  violations: Violation[];
}) {
  const map = useMap();

  useEffect(() => {
    const valid = violations.filter(
      (v) =>
        v.latitude !== null &&
        v.longitude !== null
    );

    if (valid.length === 0) return;

    const bounds = L.latLngBounds(
      valid.map(
        (v) =>
          [v.latitude!, v.longitude!] as [
            number,
            number
          ]
      )
    );

    map.fitBounds(bounds, {
      padding: [30, 30],
    });
  }, [violations, map]);

  return null;
}

export default function ViolationMap({
  violations,
  onSelect,
}: {
  violations: Violation[];
  onSelect: (violation: Violation) => void;
}) {
  const validViolations = violations.filter(
    (v) =>
      v.latitude !== null &&
      v.longitude !== null
  );

  const defaultCenter: [number, number] = [
    23.2599,
    77.4126,
  ];

  return (
    <div className="real-map">

      <MapContainer
        center={defaultCenter}
        zoom={5}
        scrollWheelZoom={true}
        style={{
          width: "100%",
          height: "100%",
        }}
      >

        <TileLayer
          attribution="&copy; OpenStreetMap contributors"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <MapController
          violations={validViolations}
        />

        {validViolations.map((violation) => (
          <Marker
            key={violation.id}
            position={[
              violation.latitude!,
              violation.longitude!,
            ]}
            icon={markerIcon}
            eventHandlers={{
              click: () => onSelect(violation),
            }}
          >
            <Popup>
              <strong>{violation.id}</strong>

              <br />

              {violation.violationType}

              <br />

              Severity: {violation.severity}

              <br />

              Status: {violation.status}
            </Popup>
          </Marker>
        ))}

      </MapContainer>

      {validViolations.length === 0 && (
        <div className="map-empty-overlay">
          <strong>
            No geo-tagged violations
          </strong>

          <span>
            GPS coordinates will appear here
            automatically.
          </span>
        </div>
      )}

    </div>
  );
}