'use strict';

const fs = require('fs');
const path = require('path');

// Load environment variables from .env if present
const envPath = path.join(__dirname, '../.env');
if (fs.existsSync(envPath)) {
  const envContent = fs.readFileSync(envPath, 'utf8');
  envContent.split('\n').forEach(line => {
    const trimmed = line.trim();
    if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
      const [key, ...vals] = trimmed.split('=');
      const k = key.trim();
      if (k && !process.env[k]) {
        process.env[k] = vals.join('=').trim();
      }
    }
  });
}

const app = require('./app');
const port = Number(process.env.PORT || 3000);

if (!Number.isInteger(port) || port < 1 || port > 65535) {
  process.stderr.write('Invalid PORT\n');
  process.exit(1);
}

app.listen(port, '0.0.0.0', () => {
  console.log(`FalconPlan backend starter listening on :${port}`);
});