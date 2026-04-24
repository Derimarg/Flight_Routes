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

  map = L.map("map", { zoomControl: true }).setView([39.0997, -94.5786], 5);
  activeTiles = (currentTheme === "dark" ? darkLayer : lightLayer).addTo(
    map,
  );

  setTimeout(() => {
    map.invalidateSize();
    renderSystem();
  }, 200);

  // Populate dropdowns with airport codes
  const srcSelect = document.getElementById("src-select");
  const destSelect = document.getElementById("dest-select");

  networkData.airports.forEach(airport => {
    let opt1 = document.createElement("option");
    let opt2 = document.createElement("option");
    opt1.value = opt2.value = airport.code;
    opt1.textContent = opt2.textContent = `${airport.code} - ${airport.name}`;
    srcSelect.appendChild(opt1);
    destSelect.appendChild(opt2);
  });
  fetchHistory();

  if (typeof lastSearch !== 'undefined') {
    window.routeData = lastSearch;
    // Give map a moment to initialize before rendering the route
    setTimeout(() => renderSystem(), 300);
  }
};

function setWeightMode(mode) {
  weightMode = mode;

  // Reset the itinerary data
  window.routeData = null;

  // Clear the terminal card back to the default message
  const itineraryBox = document.getElementById("itinerary-box");
  itineraryBox.innerHTML = `
    <div style="font-size: 0.8rem; font-weight: bold; color: var(--text-muted);">
        Mode changed. Click 'Find Best Path' to update.
    </div>`;

  // Update UI button states
  document.getElementById("btn-price").classList.toggle("active", mode === "price");
  document.getElementById("btn-duration").classList.toggle("active", mode === "duration");

  renderSystem();
}

function renderSystem() {
  if (!map) return;

  // Clear previous layers (Markers, Polylines, etc.)
  map.eachLayer((l) => {
    if (l instanceof L.Path || l instanceof L.Marker) map.removeLayer(l);
  });

  if (typeof networkData === "undefined") return;
  const isDark = document.body.getAttribute("data-theme") === "dark";

  // Draw Global Network (Background Edges + Labels)
  networkData.routes.forEach((r) => {
    const labelText = weightMode === "price" ? `$${r.price}` : `${r.duration}m`;

    const line = L.polyline(
      [[r.srcLat, r.srcLong], [r.destLat, r.destLong]],
      {
        color: isDark ? "#374151" : "#cbd5e0",
        weight: 2,
        opacity: 1,
        lineJoin: "round",
      }
    ).addTo(map);

    line.bindTooltip(labelText, {
      permanent: true,
      direction: "center",
      className: "edge-label",
      opacity: 0.9
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
    }).addTo(map).bindPopup(`<b>${a.code}</b><br>${a.name}`);
  });

  // Draw Active Route & Update Itinerary Card
  const itineraryBox = document.getElementById("itinerary-box");

  if (window.routeData && window.routeData.flights && window.routeData.flights.length > 0) {
    const pathCoords = [];
    let stepsHtml = "";

    window.routeData.flights.forEach((f, i) => {
      const start = [f.srcLat, f.srcLong];
      const end = [f.destLat, f.destLong];
      pathCoords.push(start, end);

      // look up the route in networkData to get the missing price and duration
      const matchedRoute = networkData.routes.find(r => r.src === f.src && r.dest === f.dest);
      const legPrice = matchedRoute ? matchedRoute.price : 0;
      const legDuration = matchedRoute ? matchedRoute.duration : 0;

      // Map Drawing: Magenta Polylines
      L.polyline([start, end], { color: "#CC338B", weight: 6, opacity: 1 }).addTo(map);

      // Map Drawing: Markers (Orange for Start, Blue for intermediate/end)
      L.circleMarker(start, {
        radius: 6,
        fillColor: i === 0 ? "#f59e0b" : "#000080",
        color: "#fff",
        weight: 2,
        fillOpacity: 1,
      }).addTo(map);

      // UI: Build Step-by-Step Details for Card
      stepsHtml += `
      <div class="route-step">
          <span>${i + 1}. ${f.src} → ${f.dest}</span>
          <span>($${legPrice.toFixed(2)} | ${legDuration} min)</span>
      </div>`;
    });

    // Time Formatting
    const totalD = window.routeData.totalDuration || 0;
    const hours = Math.floor(totalD / 60);
    const mins = totalD % 60;

    // UI: Update Itinerary Card with Terminal Style
    const firstAirport = window.routeData.flights[0].src;
    const lastAirport = window.routeData.flights[window.routeData.flights.length - 1].dest;

    itineraryBox.innerHTML = `
      <div class="itinerary-header">
          =======================<br>
          ITINERARY: ${firstAirport} >>> ${lastAirport}<br>
          =======================
      </div>
      <div class="itinerary-summary">
          <strong>SUMMARY:</strong><br>
          - Total Price: &nbsp;&nbsp;$ ${window.routeData.totalCost.toFixed(2)}<br>
          - Est. Duration: ${hours} hrs ${mins} mins<br>
          - Connections: &nbsp;${window.routeData.flights.length - 1}
      </div>
      <div class="itinerary-details">
          <strong>ROUTE DETAILS:</strong><br>
          ${stepsHtml}
      </div>
    `;

    // Map Zoom/Pan Adjustment
    if (pathCoords.length > 0) {
      map.fitBounds(L.polyline(pathCoords).getBounds(), { padding: [50, 50] });
    }

  } else {
    // UI: Reset to default if no path is found
    itineraryBox.innerHTML = `<div id="stat-line" style="font-size: 0.8rem; font-weight: bold">No active path.</div>`;
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

async function handleRouteSearch() {
  console.log("Button Clicked!");

  const srcSelect = document.getElementById("src-select");
  const destSelect = document.getElementById("dest-select");

  if (!srcSelect || !destSelect) {
    console.error("ERROR: Could not find the dropdown elements in the HTML!");
    return;
  }

  const src = srcSelect.value;
  const dest = destSelect.value;

  console.log(`Source: ${src}, Destination: ${dest}, Mode: ${weightMode}`);

  if (src === dest) {
    alert("Source and Destination cannot be the same!");
    return;
  }

  const url = `http://localhost:8080/findPath?source=${src}&dest=${dest}&mode=${weightMode}`;

  try {
    const response = await fetch(url);
    const data = await response.json(); // Data now has { currentRoute, history }

    // Update the active map route
    window.routeData = data.currentRoute;

    // Update the history list using the list Java generates
    renderHistoryList(data.history);

    renderSystem();
  } catch (error) {
    console.error("Fetch failed", error);
  }
}

function renderHistoryList(historyArray) {
  const historyBox = document.getElementById("history-list");
  if (!historyBox) return;

  // If there is no history data, don't clear the box; just leave the "No history" message
  if (!historyArray || historyArray.length === 0) return;

  historyBox.innerHTML = ""; // Now clear it only if we have data to show

  historyArray.forEach((route) => {
    // Safety check: ensure flights exist before trying to read index 0
    if (!route.flights || route.flights.length === 0) return;

    const start = route.flights[0].src;
    const end = route.flights[route.flights.length - 1].dest;

    const card = document.createElement("div");
    card.className = "history-item";

    if (window.routeData && JSON.stringify(window.routeData) === JSON.stringify(route)) {
      card.classList.add("active-history");
    }

    // Format minutes for the history meta
    const h = Math.floor(route.totalDuration / 60);
    const m = route.totalDuration % 60;
    const timeStr = h > 0 ? `${h}h ${m}m` : `${m}m`;

    card.innerHTML = `
        <div class="history-route"><strong>${start}</strong> → <strong>${end}</strong></div>
        <div class="history-meta">$${route.totalCost.toFixed(2)} | ${timeStr}</div>
    `;

    card.onclick = () => {
      window.routeData = route;
      renderSystem();
      renderHistoryList(historyArray);
    };

    historyBox.appendChild(card);
  });
}

function calculatePathOnWeb(startNode, endNode, mode) {
  const nodes = networkData.airports;
  const edges = networkData.routes;

  let distances = {};
  let prev = {};
  let pq = new Set();

  nodes.forEach(a => {
    distances[a.code] = Infinity;
    pq.add(a.code);
  });
  distances[startNode] = 0;

  while (pq.size > 0) {
    let u = Array.from(pq).reduce((min, code) =>
      distances[code] < distances[min] ? code : min);

    pq.delete(u);
    if (u === endNode) break;

    edges.filter(e => e.src === u).forEach(edge => {
      let alt = distances[u] + (mode === "price" ? edge.price : edge.duration);
      if (alt < distances[edge.dest]) {
        distances[edge.dest] = alt;
        prev[edge.dest] = edge;
      }
    });
  }

  // Construct the result for the map
  let path = [];
  let curr = endNode;
  while (prev[curr]) {
    path.unshift(prev[curr]);
    curr = prev[curr].src;
  }

  // Update the routeData globally and re-render
  window.routeData = {
    totalCost: distances[endNode],
    flights: path
  };
  renderSystem();
}

async function fetchHistory() {
  try {
    // Calling /getHistory prevents the "No route found" error on startup
    const response = await fetch("http://localhost:8080/getHistory");
    const data = await response.json();

    if (data.history && data.history.length > 0) {
      renderHistoryList(data.history);
    }
  } catch (e) {
    console.error("History load failed:", e);
  }
}