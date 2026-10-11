'use strict';
const app = require('./app');
const port = Number(process.env.PORT || 3000);
if (!Number.isInteger(port) || port < 1 || port > 65535) {
  process.stderr.write('Invalid PORT\n'); process.exit(1);
}
app.listen(port, '0.0.0.0', () => {
  console.log(`FalconPlan backend starter on :${port} (health endpoint only; no business flows yet)`);
});
