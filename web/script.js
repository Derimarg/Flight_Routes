let weightMode = "price";
let map, activeTiles;
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

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

function injectSVGDefs() {
  const svg = document.querySelector("#map svg");
  if (!svg || svg.querySelector("defs")) return;

  const defs = document.createElementNS("http://www.w3.org/2000/svg", "defs");
  defs.innerHTML = `
    <marker id="arrowhead" viewBox="0 0 10 10" refX="15" refY="5" markerWidth="8" markerHeight="8" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 z" />
    </marker>
    <marker id="arrowhead-active" viewBox="0 0 10 10" refX="12" refY="5" markerWidth="5" markerHeight="5" orient="auto">
      <path d="M 0 0 L 10 5 L 0 10 z" />
    </marker>`;
  svg.appendChild(defs);
}

function renderSystem() {
  if (!map) return;

  // Clear previous layers
  map.eachLayer((l) => {
    if (l instanceof L.Path || l instanceof L.Marker) map.removeLayer(l);
  });

  injectSVGDefs();


  if (typeof networkData === "undefined") return;
  const isDark = document.body.getAttribute("data-theme") === "dark";

  // Draw Global Network (Background Edges + Labels)
  networkData.routes.forEach((route) => {
    const start = [route.srcLat, route.srcLong];
    const end = [route.destLat, route.destLong];

    // Draw the line with the arrowhead class
    L.polyline([start, end], {
      color: "var(--accent-primary)",
      weight: 1.5,
      opacity: 0.3,
      className: "polyline-arrow"
    }).addTo(map);

    // DRAW WEIGHT LABELS for background
    const midLat = (route.srcLat + route.destLat) / 2;
    const midLng = (route.srcLong + route.destLong) / 2;
    const labelText = weightMode === "price" ? `$${route.price}` : `${route.duration}m`;

    L.marker([midLat, midLng], {
      icon: L.divIcon({
        className: "edge-label",
        html: labelText,
        iconSize: [30, 15],
        iconAnchor: [15, 7]
      }),
      interactive: false
    }).addTo(map);
  });

  // Draw Airports (Nodes)
  networkData.airports.forEach((a) => {
    L.circleMarker([a.lat, a.lng], {
      radius: 5,
      fillColor: isDark ? "#415356" : "#004b98",
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

      const matchedRoute = networkData.routes.find(r => r.src === f.src && r.dest === f.dest);
      const legPrice = matchedRoute ? matchedRoute.price : 0;
      const legDuration = matchedRoute ? matchedRoute.duration : 0;

      // Draw active polyline with arrow
      L.polyline([start, end], {
        color: "#CC338B", // Magenta
        weight: 5,
        opacity: 1,
        className: "active-path-arrow"
      }).addTo(map);

      // DRAW ACTIVE WEIGHT LABELS (Price or Duration)
      const midLat = (f.srcLat + f.destLat) / 2;
      const midLng = (f.srcLong + f.destLong) / 2;
      const labelText = weightMode === "price" ? `$${legPrice}` : `${legDuration}m`;

      L.marker([midLat, midLng], {
        icon: L.divIcon({
          className: "edge-label active-label", // Custom class for highlighting
          html: labelText,
          iconSize: [35, 18],
          iconAnchor: [17, 9],
          fillColor: "red"
        }),
        interactive: false,
        zIndexOffset: 1000 // Ensure it's on top
      }).addTo(map);

      // Highlight nodes of the active path
      L.circleMarker(start, {
        radius: 6,
        fillColor: i === 0 ? "#f59e0b" : "#CC338B",
        color: "#f59e0b",
        weight: 2,
        fillOpacity: 1,
        zIndexOffset: 1100
      }).addTo(map);

      stepsHtml += `
      <div class="route-step">
          <span>${i + 1}. ${f.src} → ${f.dest}</span>
          <span>($${legPrice.toFixed(2)} | ${legDuration} min)</span>
      </div>`;
    });

    // Update UI Card
    const totalD = window.routeData.totalDuration || 0;
    const hours = Math.floor(totalD / 60);
    const mins = totalD % 60;
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
      </div>`;

    if (pathCoords.length > 0) {
      map.fitBounds(L.polyline(pathCoords).getBounds(), { padding: [50, 50] });
    }
  } else {
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
  const srcSelect = document.getElementById("src-select");
  const destSelect = document.getElementById("dest-select");
  const src = srcSelect.value;
  const dest = destSelect.value;

  if (src === dest) {
    alert("Source and Destination cannot be the same!");
    return;
  }

  // Only call the visualizer. 
  // It handles the backend sync and the final render itself.
  await visualizeDijkstra(src, dest, weightMode);
}

async function visualizeDijkstra(startNode, endNode, mode) {
  const nodes = networkData.airports;
  const edges = networkData.routes;
  let distances = {};
  let prev = {};
  let pq = new Set();
  let exploredLayers = []; // Keep track of lines to clear later



  window.routeData = null;
  renderSystem(); // Clear map to start fresh

  // INITIAL UI FEEDBACK
  document.getElementById("itinerary-box").innerHTML = `
    <div class="computing-text">
        > EXECUTING DIJKSTRA...<br>
        > MODE: ${mode.toUpperCase()}<br>
        > SCANNING NETWORK...
    </div>`;

  await sleep(200);

  // Initialize distances
  nodes.forEach(a => {
    distances[a.code] = Infinity;
    pq.add(a.code);
  });
  distances[startNode] = 0;

  //  SEARCH PHASE (The "Wave" expansion)
  while (pq.size > 0) {
    let u = Array.from(pq).reduce((min, code) =>
      distances[code] < distances[min] ? code : min
    );

    if (distances[u] === Infinity) break;
    pq.delete(u);

    const airport = nodes.find(n => n.code === u);

    // Mark node as finalized (the "closed set")
    L.circleMarker([airport.lat, airport.lng], {
      className: 'node-finalized'
    }).addTo(map);

    // Current Working Node Pulse
    const currentPulse = L.circleMarker([airport.lat, airport.lng], {
      className: 'node-checking'
    }).addTo(map);

    if (u === endNode) {
      map.removeLayer(currentPulse);
      break;
    }

    const neighbors = edges.filter(e => e.src === u);
    for (const edge of neighbors) {
      // The Active "Searchlight" line
      const scanLine = L.polyline([[edge.srcLat, edge.srcLong], [edge.destLat, edge.destLong]], {
        className: 'edge-active-scan'
      }).addTo(map);

      await sleep(100);

      let alt = distances[u] + (mode === "price" ? edge.price : edge.duration);
      if (alt < distances[edge.dest]) {
        distances[edge.dest] = alt;
        prev[edge.dest] = edge;

        // Leave a faint "explored" line to show the web of search
        const exploredLine = L.polyline([[edge.srcLat, edge.srcLong], [edge.destLat, edge.destLong]], {
          className: 'edge-exploring'
        }).addTo(map);
        exploredLayers.push(exploredLine);
      }

      map.removeLayer(scanLine);
    }

    map.removeLayer(currentPulse);
    await sleep(100);
  }

  //  BACKTRACKING PHASE (The "Decision" Trace)
  // Clear the faint exploration lines to make the backtrack pop
  exploredLayers.forEach(layer => map.removeLayer(layer));

  let path = [];
  let curr = endNode;

  document.getElementById("itinerary-box").innerHTML = `
    <div class="computing-text" style="color: #ff0055;">
        > TARGET FOUND: ${endNode}<br>
        > BACKTRACKING OPTIMAL PATH...
    </div>`;

  await sleep(200);

  while (prev[curr]) {
    let edge = prev[curr];
    path.unshift(edge);

    // Draw the backtrack segment in bold pink
    const backtrackLine = L.polyline([[edge.srcLat, edge.srcLong], [edge.destLat, edge.destLong]], {
      className: 'edge-backtracking'
    }).addTo(map);

    // Highlight the node being reached in the backtrack
    const nodeHighlight = L.circleMarker([edge.srcLat, edge.srcLong], {
      radius: 8,
      color: '#ff0055',
      fillColor: '#ff0055',
      fillOpacity: 1,
      zIndexOffset: 1000
    }).addTo(map);

    await sleep(450); // Slower pace so the user can follow the path home
    curr = edge.src;
  }

  await sleep(500); // Dramatic pause before final UI update

  // FINAL SYNC & RENDER
  // We fetch from the backend now to ensure history and DB are updated
  const url = `http://localhost:8080/findPath?source=${startNode}&dest=${endNode}&mode=${mode}`;
  try {
    const response = await fetch(url);
    const data = await response.json();

    window.routeData = data.currentRoute;
    renderHistoryList(data.history);
    renderSystem(); // Final clean render with Magenta paths and Itinerary cards
  } catch (error) {
    console.error("Final sync failed:", error);
  }
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

function renderHistoryList(historyArray) {
  const historyBox = document.getElementById("history-list");
  if (!historyBox) return;


  historyBox.innerHTML = "";

  if (!historyArray || historyArray.length === 0) {
    historyBox.innerHTML = `<div style="font-size: 0.75rem; color: var(--text-muted); text-align: center;">No history yet.</div>`;
    return;
  }

  historyArray.forEach((route) => {
    if (!route.flights || route.flights.length === 0) return;

    const startCode = route.flights[0].src;
    const endCode = route.flights[route.flights.length - 1].dest;

    const card = document.createElement("div");
    card.className = "history-item";

    // Keep your existing logic for active-history and onclick...
    if (window.routeData && JSON.stringify(window.routeData) === JSON.stringify(route)) {
      card.classList.add("active-history");
    }

    card.onclick = () => {
      window.routeData = route;
      renderSystem();
      document.getElementById("src-select").value = startCode;
      document.getElementById("dest-select").value = endCode;
      renderHistoryList(historyArray);
    };

    const h = Math.floor(route.totalDuration / 60);
    const m = route.totalDuration % 60;
    card.innerHTML = `
        <div class="history-route"><strong>${startCode}</strong> → <strong>${endCode}</strong></div>
        <div class="history-meta">$${route.totalCost.toFixed(2)} | ${h}h ${m}m</div>
    `;

    historyBox.appendChild(card);
  });
}