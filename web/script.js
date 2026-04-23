let weightMode = "price";
let map, activeTiles;

const lightLayer = L.tileLayer(
  "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png",
);
const darkLayer = L.tileLayer(
  "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png",
);

// Initialize on window load to ensure all containers have dimensions
window.onload = function () {
  let currentTheme =
    typeof systemSettings !== "undefined"
      ? systemSettings.theme
      : "light";
  document.body.setAttribute("data-theme", currentTheme);

  map = L.map("map", { zoomControl: false }).setView([39.8, -98.5], 4);
  activeTiles = (currentTheme === "dark" ? darkLayer : lightLayer).addTo(
    map,
  );

  setTimeout(() => {
    map.invalidateSize();
    renderSystem();
  }, 200);
};

function setWeightMode(mode) {
  weightMode = mode;
  document
    .getElementById("btn-price")
    .classList.toggle("active", mode === "price");
  document
    .getElementById("btn-duration")
    .classList.toggle("active", mode === "duration");
  renderSystem();
}

function renderSystem() {
  if (!map) return;

  // Check if data actually exists
  if (typeof networkData === "undefined") {
    console.error(
      "Data check failed: networkData is not defined. check your script tags.",
    );
    return;
  }

  // Calculate bounds of all airports and fit the map to them
  const allAirports = networkData.airports.map(a => [a.lat, a.lng]);
  map.fitBounds(allAirports, { padding: [40, 40] });

  // Clear all dynamic layers
  map.eachLayer((l) => {
    if (l instanceof L.Path || l instanceof L.Marker) map.removeLayer(l);
  });

  const isDark = document.body.getAttribute("data-theme") === "dark";

  // Draw Global Network (Edges)
  if (typeof networkData !== "undefined" && networkData.routes) {
    networkData.routes.forEach((r) => {
      const val =
        weightMode === "price" ? `$${r.price}` : `${r.duration}m`;

      const line = L.polyline(
        [
          [r.srcLat, r.srcLong],
          [r.destLat, r.destLong],
        ],
        {
          color: isDark ? "#374151" : "#cbd5e0",
          weight: 1.5,
          opacity: 0.5,
        },
      ).addTo(map);

      line.bindTooltip(val, {
        permanent: true, // always visible without hovering
        direction: "center",
        // sticky: true,
        className: "edge-label"
      });
    });

    // Draw Airports (Nodes)
    networkData.airports.forEach((a) => {
      L.circleMarker([a.lat, a.lng], {
        radius: 4,
        fillColor: isDark ? "#00d4ff" : "#004b98",
        color: "#fff",
        weight: 1,
        fillOpacity: 0.8,
      })
        .addTo(map)
        .bindPopup(`<b>${a.code}</b><br>${a.name}`);
    });
  }

  // Draw Active Route
  if (typeof routeData !== "undefined" && routeData.flights) {
    const pathCoords = [];
    routeData.flights.forEach((f, i) => {
      const start = [f.srcLat, f.srcLong];
      const end = [f.destLat, f.destLong];

      L.polyline([start, end], { color: "#10b981", weight: 5 }).addTo(
        map,
      );

      L.circleMarker(start, {
        radius: 6,
        fillColor: i === 0 ? "#f59e0b" : "#10b981",
        color: "#fff",
        weight: 2,
        fillOpacity: 1,
      }).addTo(map);

      pathCoords.push(start, end);
    });

    document.getElementById("stat-line").innerHTML =
      `TOTAL COST: $${routeData.totalCost}<br>DURATION: ${Math.floor(routeData.totalDuration / 60)}H ${routeData.totalDuration % 60}M`;

    if (pathCoords.length > 0)
      map.fitBounds(L.polyline(pathCoords).getBounds(), {
        padding: [50, 50],
      });
  }
}

function toggleTheme() {
  const theme =
    document.body.getAttribute("data-theme") === "dark"
      ? "light"
      : "dark";
  document.body.setAttribute("data-theme", theme);
  map.removeLayer(activeTiles);
  activeTiles = (theme === "dark" ? darkLayer : lightLayer).addTo(map);
  renderSystem();
}
