'use strict';

const { test, describe } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const app = require('../src/app');

function makeRequest(path, options = {}) {
  return new Promise((resolve, reject) => {
    const server = http.createServer(app);
    server.listen(0, '127.0.0.1', () => {
      const port = server.address().port;
      const req = http.request({
        host: '127.0.0.1',
        port,
        path,
        method: options.method || 'GET',
        headers: options.headers || {}
      }, (res) => {
        let body = '';
        res.on('data', chunk => body += chunk);
        res.on('end', () => {
          server.close();
          let json = null;
          try {
            json = JSON.parse(body);
          } catch (_) {}
          resolve({ status: res.statusCode, headers: res.headers, body, json });
        });
      });

      req.on('error', (err) => {
        server.close();
        reject(err);
      });

      if (options.body) {
        req.write(options.body);
      }
      req.end();
    });
  });
}

describe('FalconPlan Backend API & Security Tests', () => {

  test('GET /api/health returns 200 OK with status ok', async () => {
    const res = await makeRequest('/api/health');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'ok');
    assert.equal(res.json.service, 'falconplan');
  });

  test('GET /api/me without Authorization header returns 401 or 503 depending on config', async () => {
    const res = await makeRequest('/api/me');
    assert.ok(res.status === 401 || res.status === 503);
    assert.ok(res.json.error);
  });

  test('GET /api/me with malformed Bearer token returns 401 or 503', async () => {
    const res = await makeRequest('/api/me', {
      headers: { 'Authorization': 'Bearer invalid_token_123' }
    });
    assert.ok(res.status === 401 || res.status === 503);
  });

  test('GET /api/me in mock test mode returns authenticated user data', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/me', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'user_falcon_789'
      }
    });
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.uid, 'user_falcon_789');
  });

  test('POST /api/me with oversized payload returns 413 Payload Too Large', async () => {
    const largeBody = JSON.stringify({ data: 'a'.repeat(70000) });
    const res = await makeRequest('/api/me', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: largeBody
    });
    assert.equal(res.status, 413);
  });

  test('GET /api/unimplemented_route returns 404 Not Found', async () => {
    const res = await makeRequest('/api/unimplemented_route');
    assert.equal(res.status, 404);
  });
});