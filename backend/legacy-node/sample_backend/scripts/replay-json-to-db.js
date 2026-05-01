#!/usr/bin/env node

/**
 * Replay committed JSONL history into PostgreSQL through the backend API.
 * Usage:
 *   node scripts/replay-json-to-db.js
 *   node scripts/replay-json-to-db.js http://127.0.0.1:3000
 */

const baseUrl = (process.argv[2] || process.env.API_BASE_URL || 'http://127.0.0.1:3000').replace(/\/$/, '');

async function main() {
  const replayUrl = `${baseUrl}/api/replay/raw-to-db`;
  const statusUrl = `${baseUrl}/api/debug/ingest-status`;

  try {
    const replayRes = await fetch(replayUrl, { method: 'POST' });
    const replayText = await replayRes.text();

    let replayJson;
    try {
      replayJson = JSON.parse(replayText);
    } catch (_err) {
      throw new Error(`Replay returned non-JSON response (${replayRes.status}): ${replayText}`);
    }

    if (!replayRes.ok) {
      throw new Error(`Replay failed (${replayRes.status}): ${JSON.stringify(replayJson)}`);
    }

    console.log('Replay completed successfully:');
    console.log(JSON.stringify(replayJson, null, 2));

    const statusRes = await fetch(statusUrl);
    const statusText = await statusRes.text();

    let statusJson;
    try {
      statusJson = JSON.parse(statusText);
    } catch (_err) {
      throw new Error(`Status returned non-JSON response (${statusRes.status}): ${statusText}`);
    }

    if (!statusRes.ok) {
      throw new Error(`Status check failed (${statusRes.status}): ${JSON.stringify(statusJson)}`);
    }

    console.log('\nCurrent ingest status:');
    console.log(JSON.stringify(statusJson, null, 2));
  } catch (err) {
    console.error('[replay-json-to-db] Error:', err.message || err);
    process.exit(1);
  }
}

main();
