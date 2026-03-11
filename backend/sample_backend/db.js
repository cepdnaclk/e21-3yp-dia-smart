const { Pool } = require('pg');

const pool = new Pool({
  user: process.env.PGUSER || 'postgres',
  host: process.env.PGHOST || 'localhost',
  database: process.env.PGDATABASE || 'diasmart1',
  password: 'Arnikan18', // <-- Change this line
  port: Number(process.env.PGPORT || 5432),
});

module.exports = pool;