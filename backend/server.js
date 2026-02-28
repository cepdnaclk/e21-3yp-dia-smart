const express = require('express');
const cors = require('cors');
const pool = require('./db');

const app = express();
app.use(cors());
app.use(express.json());

/* ===============================
   POST - Insert Sensor Data
================================= */
app.post('/api/readings', async (req, res) => {

  const {
    temperature,
    door_status,
    insulin_inventory_weight,
    insulin_level_value,
    glucose_value
  } = req.body;

  try {
    await pool.query(
      `INSERT INTO readings 
      (temperature, door_status, insulin_inventory_weight, insulin_level_value, glucose_value)
      VALUES ($1, $2, $3, $4, $5)`,
      [temperature, door_status, insulin_inventory_weight, insulin_level_value, glucose_value]
    );

    res.status(200).json({ message: "Data inserted successfully" });

  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Database error" });
  }
});


/* ===============================
   GET - All Readings
================================= */
app.get('/api/readings', async (req, res) => {

  const result = await pool.query(
    `SELECT * FROM readings ORDER BY created_at DESC LIMIT 100`
  );

  res.json(result.rows);
});

// Get latest reading
app.get('/api/latest', async (req, res) => {
  const result = await pool.query(
    `SELECT * FROM readings
     ORDER BY created_at DESC
     LIMIT 1`
  );
  res.json(result.rows[0]);
});

// Get readings for graph
app.get('/api/history', async (req, res) => {
  const result = await pool.query(
    `SELECT created_at, temperature, glucose_value,
            insulin_inventory_weight
     FROM readings
     ORDER BY created_at ASC
     LIMIT 200`
  );
  res.json(result.rows);
});


app.listen(3000, () => {
  console.log("Server running on port 3000");
});