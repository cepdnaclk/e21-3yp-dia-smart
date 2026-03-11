const { Pool } = require('pg');

const pool = new Pool({
  user: process.env.PGUSER || 'postgres',
  host: process.env.PGHOST || 'localhost',
  database: process.env.PGDATABASE || 'diasmart',
  password: process.env.PGPASSWORD || 'postgres',
  port: Number(process.env.PGPORT || 5432),
});

module.exports = pool;