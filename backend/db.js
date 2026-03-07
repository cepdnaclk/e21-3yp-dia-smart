const { Pool } = require('pg');

const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'diasmart',
  password: 'Sanjeevan2002',
  port: 5432,
});

module.exports = pool;