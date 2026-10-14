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

  // --- PROVIDER LISTING SUBMISSION & ADMIN MODERATION TESTS (PHASE 04-B3-1) ---

  test('POST /api/provider/listings/:listingId/submit with unverified KYC returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/listings/listing_123/submit', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'unverified_provider_123',
        'x-test-mock-unverified-kyc': 'true'
      }
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('POST /api/provider/listings/:listingId/submit with missing photos returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/listings/listing_123/submit', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'verified_provider_123',
        'x-test-mock-missing-photos': 'true'
      }
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('POST /api/provider/listings/:listingId/submit for verified provider transitions status to submitted_under_review', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/listings/listing_123/submit', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'verified_provider_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'submitted_under_review');
  });

  test('GET /api/admin/listings without admin claims returns 403 FORBIDDEN', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/listings', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'ordinary_user_123'
      }
    });

    assert.equal(res.status, 403);
    assert.equal(res.json.code, 'FORBIDDEN');
  });

  test('GET /api/admin/listings with admin claims returns 200 OK', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/listings', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('POST /api/admin/listings/:listingId/decision approve decision updates status to published', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ providerUid: 'verified_provider_123', decision: 'approve' });

    const res = await makeRequest('/api/admin/listings/listing_123/decision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'published');
  });

  test('POST /api/admin/listings/:listingId/decision reject decision without reason returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ providerUid: 'verified_provider_123', decision: 'reject' });

    const res = await makeRequest('/api/admin/listings/listing_123/decision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('GET /api/public-listings returns paginated list of approved published listings', async () => {
    const res = await makeRequest('/api/public-listings?page=1&limit=10');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.page, 1);
    assert.equal(res.json.limit, 10);
    assert.ok(Array.isArray(res.json.data));
  });

  // --- PROVIDER LISTING PHOTO ENDPOINTS TESTS ---

  test('GET /api/provider/listings/:listingId/photos returns photos list', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/listings/listing_123/photos', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('GET /api/provider/listings/:listingId/photos/:photoId/preview returns short-lived authorized preview token', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/listings/listing_123/photos/photo_123/preview', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.photoId, 'photo_123');
    assert.ok(res.json.data.authorizedPreviewUrl);
    assert.ok(res.json.data.expiresAt > Date.now());
  });

  test('POST /api/provider/listings/:listingId/photos uploads listing photo and returns metadata', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      mimeType: 'image/jpeg',
      fileBase64: '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP123456789012345678901234567890',
      isCover: true
    });

    const res = await makeRequest('/api/provider/listings/listing_123/photos', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.mimeType, 'image/jpeg');
    assert.ok(res.json.data.photoId);
  });

  test('POST /api/provider/listings/:listingId/photos exceeding 10 max photos limit returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      mimeType: 'image/jpeg',
      fileBase64: '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP123456789012345678901234567890'
    });

    const res = await makeRequest('/api/provider/listings/listing_123/photos', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123',
        'x-test-mock-max-exceeded': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('PATCH /api/provider/listings/:listingId/photos/reorder reorders photos', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      photoOrder: ['photo_2', 'photo_1']
    });

    const res = await makeRequest('/api/provider/listings/listing_123/photos/reorder', {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
  });

  test('PATCH /api/provider/listings/:listingId/photos/:photoId/cover sets cover photo', async () => {
    process.env.NODE_ENV = 'test';

    const res = await makeRequest('/api/provider/listings/listing_123/photos/photo_123/cover', {
      method: 'PATCH',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.coverPhotoId, 'photo_123');
  });

  test('DELETE /api/provider/listings/:listingId/photos/:photoId deletes listing photo', async () => {
    process.env.NODE_ENV = 'test';

    const res = await makeRequest('/api/provider/listings/listing_123/photos/photo_123', {
      method: 'DELETE',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.photoId, 'photo_123');
  });

  // --- ADMIN KYC REVIEW ENDPOINTS TESTS ---

  test('GET /api/admin/kyc without admin claims returns 403 FORBIDDEN', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/kyc', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'ordinary_user_123'
      }
    });

    assert.equal(res.status, 403);
    assert.equal(res.json.code, 'FORBIDDEN');
  });

  test('GET /api/admin/kyc with admin claims returns 200 OK and paginated list', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/kyc', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('POST /api/admin/kyc/:providerUid/decision approve decision updates status to verified', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'approve' });

    const res = await makeRequest('/api/admin/kyc/provider_user_123/decision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'verified');
  });

  test('POST /api/admin/kyc/:providerUid/decision reject decision without reason returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'reject' });

    const res = await makeRequest('/api/admin/kyc/provider_user_123/decision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('POST /api/admin/kyc/:providerUid/decision reject decision with valid reason returns 200 OK', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'reject', reason: 'NIC back side image was unreadable.' });

    const res = await makeRequest('/api/admin/kyc/provider_user_123/decision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'rejected');
  });

  // --- KYC ENDPOINTS TESTS ---

  test('GET /api/provider/kyc/status returns provider KYC status', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/kyc/status', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.providerUid, 'provider_user_123');
    assert.equal(res.json.data.status, 'pending_identity_verification');
  });

  test('POST /api/provider/kyc/documents uploads document and returns metadata', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      documentType: 'nic_front',
      mimeType: 'image/jpeg',
      fileBase64: '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP123456789012345678901234567890'
    });

    const res = await makeRequest('/api/provider/kyc/documents', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.type, 'nic_front');
    assert.ok(res.json.data.documentId);
  });

  test('POST /api/provider/kyc/documents with file signature mismatch returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      documentType: 'nic_front',
      mimeType: 'image/jpeg',
      fileBase64: 'INVALID_HEADER_NOT_JPEG_BYTES_12345678901234567890'
    });

    const res = await makeRequest('/api/provider/kyc/documents', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123',
        'x-test-mock-bad-signature': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('POST /api/provider/kyc/documents with invalid documentType returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      documentType: 'hacked_doc_type',
      mimeType: 'image/jpeg',
      fileBase64: '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP'
    });

    const res = await makeRequest('/api/provider/kyc/documents', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('DELETE /api/provider/kyc/documents/:documentId deletes document', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/kyc/documents/doc_nic_front_123', {
      method: 'DELETE',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.documentId, 'doc_nic_front_123');
  });

  test('POST /api/provider/kyc/submit with missing NIC back side returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/kyc/submit', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123',
        'x-test-mock-missing-nic-back': 'true'
      }
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('POST /api/provider/kyc/submit updates KYC status to submitted_under_review', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/kyc/submit', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'submitted_under_review');
  });

  // --- PUBLIC TRAVEL PLANS ENDPOINTS TESTS ---

  test('POST /api/plans/:planId/publish publishes sanitized public plan and strips private notes', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      title: 'Ella & Knuckles Community Trek',
      description: 'A 5-day village stay',
      ownerDisplayName: 'Saman Perera',
      stops: [{
        destinationId: 'dest_sigiriya',
        destinationName: 'Sigiriya',
        district: 'Matale',
        latitude: 7.957,
        longitude: 80.76,
        notes: 'TOP SECRET PRIVATE ENTRY NOTE' // Should be stripped out!
      }]
    });

    const res = await makeRequest('/api/plans/plan_001/publish', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'user_falcon_789'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.publicPlanId, 'plan_001');
    assert.equal(res.json.data.ownerUid, 'user_falcon_789');
    assert.equal(res.json.data.status, 'published');

    // Verify private notes were stripped out in public snapshot!
    const publishedStop = res.json.data.stops[0];
    assert.equal(publishedStop.destinationId, 'dest_sigiriya');
    assert.equal(publishedStop.notes, undefined);
  });

  test('POST /api/plans/:planId/publish without auth header returns 401 or 503', async () => {
    delete process.env.NODE_ENV;
    const res = await makeRequest('/api/plans/plan_001/publish', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: 'Test Plan' })
    });
    assert.ok(res.status === 401 || res.status === 503);
  });

  test('POST /api/plans/:planId/publish by non-owner returns 403 FORBIDDEN', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/plans/plan_001/publish', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'user_unauthorized_999',
        'x-test-mock-unauthorized': 'true'
      },
      body: JSON.stringify({ title: 'Test Plan' })
    });
    assert.equal(res.status, 403);
    assert.equal(res.json.code, 'FORBIDDEN');
  });

  test('POST /api/plans/:planId/unpublish unpublishes public plan', async () => {
    process.env.NODE_ENV = 'test';

    const res = await makeRequest('/api/plans/plan_001/unpublish', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'user_falcon_789'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.planId, 'plan_001');
  });

  test('POST /api/plans/:planId/unpublish by non-owner returns 403 FORBIDDEN', async () => {
    process.env.NODE_ENV = 'test';

    const res = await makeRequest('/api/plans/plan_001/unpublish', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'user_unauthorized_999',
        'x-test-mock-unauthorized': 'true'
      }
    });

    assert.equal(res.status, 403);
    assert.equal(res.json.code, 'FORBIDDEN');
  });

  test('GET /api/public-plans returns paginated list of public plans', async () => {
    const res = await makeRequest('/api/public-plans?page=1&limit=10');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.page, 1);
    assert.equal(res.json.limit, 10);
    assert.ok(Array.isArray(res.json.data));
  });

  test('GET /api/public-plans/:publicPlanId for non-existent plan returns 404 NOT_FOUND', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/public-plans/non_existent_plan_999');
    assert.equal(res.status, 404);
    assert.equal(res.json.code, 'NOT_FOUND');
  });

  test('GET /api/public-plans/:publicPlanId for published plan returns 200 OK', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/public-plans/plan_sigiriya_1', {
      headers: { 'x-test-mock-published': 'true' }
    });
    assert.equal(res.status, 200);
    assert.equal(res.json.data.publicPlanId, 'plan_sigiriya_1');
    assert.equal(res.json.data.status, 'published');
  });

  test('POST /api/me with oversized payload returns 413 Payload Too Large', async () => {
    const largeBody = JSON.stringify({ data: 'a'.repeat(6000000) });
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