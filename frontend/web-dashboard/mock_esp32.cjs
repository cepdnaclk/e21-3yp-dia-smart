const http = require("http");

let currentStatus = "idle";
let timerId = null;

const server = http.createServer((req, res) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");

  if (req.method === "OPTIONS") {
    res.writeHead(204);
    res.end();
    return;
  }

  if (req.url === "/api/provision" && req.method === "POST") {
    let body = "";
    req.on("data", (chunk) => {
      body += chunk.toString();
    });
    req.on("end", () => {
      console.log("\n[ESP32] Received provision credentials request:", body);
      currentStatus = "connecting";

      if (timerId) clearTimeout(timerId);
      timerId = setTimeout(() => {
        currentStatus = "success";
        console.log("[ESP32] Provisioning completed successfully!");
      }, 6000);

      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ success: true, message: "Credentials received" }));
    });
  } else if (req.url === "/api/provision/status" && req.method === "GET") {
    res.writeHead(200, { "Content-Type": "application/json" });
    
    const responseData = {
      status: currentStatus,
      outerStatus:
        currentStatus === "success"
          ? "CONNECTED"
          : currentStatus === "connecting"
          ? "CONNECTING"
          : "PENDING",
      innerStatus: currentStatus === "success" ? "CONNECTED" : "PENDING",
      message:
        currentStatus === "connecting"
          ? "Connecting to local router and verifying security handshake..."
          : "Base station idle.",
    };
    
    res.end(JSON.stringify(responseData));
  } else {
    res.writeHead(404);
    res.end();
  }
});

const PORT = 80;
server.listen(PORT, "127.0.0.1", () => {
  console.log(`\n======================================================`);
  console.log(`ESP32 Mock Server running at http://127.0.0.1:${PORT}`);
  console.log(`======================================================`);
  console.log(`To route requests from '192.168.4.1' to this local mock:`);
  console.log(`1. Open C:\\Windows\\System32\\drivers\\etc\\hosts in Administrator mode.`);
  console.log(`2. Add this line at the bottom:`);
  console.log(`   127.0.0.1  192.168.4.1`);
  console.log(`3. Save the file.`);
  console.log(`======================================================\n`);
});
