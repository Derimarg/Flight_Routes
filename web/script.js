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
      weight: 1,
      opacity: 0.15,
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

  // 2. Draw Airports (Nodes)
  networkData.airports.forEach((a) => {
    L.circleMarker([a.lat, a.lng], {
      radius: 5,
      fillColor: isDark ? "#415356" : "#004b98",
      color: "#fff",
      weight: 1,
      fillOpacity: 0.8,
    }).addTo(map).bindPopup(`<b>${a.code}</b><br>${a.name}`);
  });

  // 3. Draw Active Route & Update Itinerary Card
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

  if (!historyArray || historyArray.length === 0) return;

  historyBox.innerHTML = "";

  historyArray.forEach((route) => {
    if (!route.flights || route.flights.length === 0) return;

    // Get the codes for the first and last airport in this specific history route
    const startCode = route.flights[0].src;
    const endCode = route.flights[route.flights.length - 1].dest;

    const card = document.createElement("div");
    card.className = "history-item";

    // Add active styling if this route matches the current routeData
    if (window.routeData && JSON.stringify(window.routeData) === JSON.stringify(route)) {
      card.classList.add("active-history");
    }

    // THE CLICK HANDLER
    card.onclick = () => {
      // 1. Update the Map & Itinerary Card
      window.routeData = route;
      renderSystem();

      // 2. FORCE THE DROPDOWNS TO CHANGE
      const srcSelect = document.getElementById("src-select");
      const destSelect = document.getElementById("dest-select");

      if (srcSelect && destSelect) {
        srcSelect.value = startCode;
        destSelect.value = endCode;
      }

      // 3. Re-render the list to update the 'active-history' border/color
      renderHistoryList(historyArray);
    };

    // Calculate time for the card text
    const h = Math.floor(route.totalDuration / 60);
    const m = route.totalDuration % 60;
    const timeStr = h > 0 ? `${h}h ${m}m` : `${m}m`;

    card.innerHTML = `
        <div class="history-route"><strong>${startCode}</strong> → <strong>${endCode}</strong></div>
        <div class="history-meta">$${route.totalCost.toFixed(2)} | ${timeStr}</div>
    `;

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