#!/usr/bin/env node

/**
 * Print current raw ingest status from backend.
 * Usage:
 *   node scripts/ingest-status.js
 *   node scripts/ingest-status.js http://127.0.0.1:3000
 */

const baseUrl = (process.argv[2] || process.env.API_BASE_URL || 'http://127.0.0.1:3000').replace(/\/$/, '');

async function main() {
  const url = `${baseUrl}/api/debug/ingest-status`;

  try {
    const res = await fetch(url);
    const text = await res.text();

    let json;
    try {
      json = JSON.parse(text);
    } catch (_err) {
      throw new Error(`Non-JSON response (${res.status}): ${text}`);
    }

    if (!res.ok) {
      throw new Error(`Request failed (${res.status}): ${JSON.stringify(json)}`);
    }

    console.log(JSON.stringify(json, null, 2));
  } catch (err) {
    console.error('[ingest-status] Error:', err.message || err);
    process.exit(1);
  }
}

main();
