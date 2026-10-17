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

  // --- COMMUNITY POSTS BACKEND TESTS (PHASE 06-A1) ---

  // --- SECURE COMMUNITY FOLLOW / UNFOLLOW BACKEND TESTS (PHASE 06-C2-A) ---

  test('PUT /api/community/profiles/:targetUid/follow follows target profile', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/profiles/target_user_456/follow', {
      method: 'PUT',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.isFollowingByMe, true);
  });

  test('PUT /api/community/profiles/:targetUid/follow following self returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/profiles/traveler_user_123/follow', {
      method: 'PUT',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('DELETE /api/community/profiles/:targetUid/follow unfollows target profile', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/profiles/target_user_456/follow', {
      method: 'DELETE',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.isFollowingByMe, false);
  });

  test('GET /api/community/profiles/:targetUid/followers returns public followers list', async () => {
    const res = await makeRequest('/api/community/profiles/target_user_456/followers');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('GET /api/community/profiles/:targetUid/followers on private target returns 404 NOT_FOUND', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/profiles/private_user_999/followers', {
      headers: {
        'x-test-mock-private': 'true'
      }
    });

    assert.equal(res.status, 404);
    assert.equal(res.json.code, 'NOT_FOUND');
  });

  test('GET /api/community/profiles/:targetUid/following returns public following list', async () => {
    const res = await makeRequest('/api/community/profiles/target_user_456/following');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('GET /api/community/profiles/:targetUid/following on private target returns 404 NOT_FOUND', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/profiles/private_user_999/following', {
      headers: {
        'x-test-mock-private': 'true'
      }
    });

    assert.equal(res.status, 404);
    assert.equal(res.json.code, 'NOT_FOUND');
  });

  // --- SECURE SAVED COMMUNITY POSTS BACKEND TESTS (PHASE 06-B3-1) ---

  test('PUT /api/community/posts/:postId/save saves published post', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/post_1001/save', {
      method: 'PUT',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.savedByMe, true);
  });

  test('DELETE /api/community/posts/:postId/save unsaves post', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/post_1001/save', {
      method: 'DELETE',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.savedByMe, false);
  });

  test('GET /api/community/saved-posts returns traveler saved published posts', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/saved-posts', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  // --- SECURE COMMUNITY COMMENTS BACKEND TESTS (PHASE 06-B2-1) ---

  test('POST /api/community/posts/:postId/comments creates comment on published post', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ text: 'Amazing travel story! Sigiriya is breathtaking.' });

    const res = await makeRequest('/api/community/posts/post_1001/comments', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.text, 'Amazing travel story! Sigiriya is breathtaking.');
  });

  test('POST /api/community/posts/:postId/comments with empty text returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ text: '   ' });

    const res = await makeRequest('/api/community/posts/post_1001/comments', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('GET /api/community/posts/:postId/comments returns public visible comments list', async () => {
    const res = await makeRequest('/api/community/posts/post_1001/comments');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('PATCH /api/community/posts/:postId/comments/:commentId updates author comment', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ text: 'Updated comment text for travel story.' });

    const res = await makeRequest('/api/community/posts/post_1001/comments/comment_101', {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.text, 'Updated comment text for travel story.');
  });

  test('DELETE /api/community/posts/:postId/comments/:commentId deletes author comment', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/post_1001/comments/comment_101', {
      method: 'DELETE',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
  });

  test('POST /api/admin/community/comments/:commentId/moderate hides comment with admin claims', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ postId: 'post_1001', decision: 'hide', reason: 'Inappropriate language' });

    const res = await makeRequest('/api/admin/community/comments/comment_101/moderate', {
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
  });

  test('POST /api/community/posts creates draft community post', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      caption: 'Trekking through Knuckles Cloud Forest!',
      destinationId: 'dest_knuckles'
    });

    const res = await makeRequest('/api/community/posts', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'draft');
    assert.equal(res.json.data.caption, 'Trekking through Knuckles Cloud Forest!');
  });

  test('POST /api/community/posts with empty caption returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ caption: '   ' });

    const res = await makeRequest('/api/community/posts', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('GET /api/community/posts/mine returns traveler posts list', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/mine', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('POST /api/community/posts/:postId/photos uploads photo for draft post', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      mimeType: 'image/jpeg',
      fileBase64: '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP123456789012345678901234567890'
    });

    const res = await makeRequest('/api/community/posts/post_1001/photos', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.mimeType, 'image/jpeg');
  });

  test('PATCH /api/community/posts/:postId updates post caption', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ caption: 'Updated caption for Ella travel story' });

    const res = await makeRequest('/api/community/posts/post_1001', {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.caption, 'Updated caption for Ella travel story');
  });

  test('DELETE /api/community/posts/:postId/photos/:photoId deletes individual photo', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/post_1001/photos/photo_101', {
      method: 'DELETE',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
  });

  test('POST /api/community/posts/:postId/like likes post atomically', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/post_1001/like', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.isLikedByMe, true);
  });

  test('POST /api/community/posts/:postId/like repeated twice is idempotent', async () => {
    process.env.NODE_ENV = 'test';
    const res1 = await makeRequest('/api/community/posts/post_1001/like', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    const res2 = await makeRequest('/api/community/posts/post_1001/like', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res1.status, 200);
    assert.equal(res2.status, 200);
    assert.equal(res1.json.data.likeCount, res2.json.data.likeCount);
  });

  test('POST /api/community/posts/:postId/unlike unlikes post atomically', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/post_1001/unlike', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.isLikedByMe, false);
  });

  test('POST /api/community/posts/:postId/unlike repeated twice never results in negative likeCount', async () => {
    process.env.NODE_ENV = 'test';
    const res1 = await makeRequest('/api/community/posts/post_1001/unlike', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    const res2 = await makeRequest('/api/community/posts/post_1001/unlike', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res1.status, 200);
    assert.equal(res2.status, 200);
    assert.ok(res2.json.data.likeCount >= 0);
  });

  test('POST /api/community/posts/:postId/publish publishes community post', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/community/posts/post_1001/publish', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'published');
  });

  test('GET /api/community/feed returns public community travel stories feed', async () => {
    const res = await makeRequest('/api/community/feed?page=1&limit=10');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('POST /api/community/posts/:postId/report reports post for moderation', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ reason: 'Inappropriate content or spam' });

    const res = await makeRequest('/api/community/posts/post_1001/report', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
  });

  test('POST /api/admin/community/posts/:postId/moderate hides post with admin claims', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'hide', reason: 'Violates community guidelines' });

    const res = await makeRequest('/api/admin/community/posts/post_1001/moderate', {
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
  });

  // --- BOOKING BACKEND & DOUBLE BOOKING PREVENTION TESTS (PHASE 05-A1) ---

  test('GET /api/public-listings/:listingId/rooms returns approved room options', async () => {
    const res = await makeRequest('/api/public-listings/listing_123/rooms');
    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data.rooms));
  });

  test('POST /api/bookings/quote calculates itemized LKR price quote server-side', async () => {
    const payload = JSON.stringify({
      listingId: 'listing_123',
      roomId: 'room_101',
      checkInDate: '2026-11-10',
      checkOutDate: '2026-11-12',
      guests: 2,
      roomUnitsRequested: 1,
      selectedBreakfast: true
    });

    const res = await makeRequest('/api/bookings/quote', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.currency, 'LKR');
    assert.equal(res.json.data.nights, 2);
    assert.ok(res.json.data.quotedTotalLkr > 0);
  });

  test('POST /api/bookings creates pending booking request', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      listingId: 'listing_123',
      roomId: 'room_101',
      checkInDate: '2026-11-10',
      checkOutDate: '2026-11-12',
      guests: 2,
      roomUnitsRequested: 1
    });

    const res = await makeRequest('/api/bookings', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'pending');
    assert.equal(res.json.data.travelerUid, 'traveler_user_123');
  });

  test('GET /api/bookings/mine returns traveler bookings list', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/bookings/mine', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('POST /api/provider/bookings/:bookingId/decision accepts booking request', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'accept' });

    const res = await makeRequest('/api/provider/bookings/booking_1001/decision', {
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
    assert.equal(res.json.data.status, 'confirmed');
  });

  test('POST /api/bookings/:bookingId/cancel cancels booking request', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/bookings/booking_1001/cancel', {
      method: 'POST',
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.status, 'cancelled');
  });

  // --- SECURE PAYMENT BACKEND TESTS (PHASE 05-B1) ---

  test('POST /api/payments/initiate with bank_transfer returns awaiting_proof status', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      bookingId: 'booking_1001',
      method: 'bank_transfer'
    });

    const res = await makeRequest('/api/payments/initiate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.method, 'bank_transfer');
    assert.equal(res.json.data.status, 'awaiting_proof');
    assert.equal(res.json.data.amountDueLkr, 19000);
  });

  test('POST /api/payments/initiate with cash_on_arrival returns due_on_arrival status', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      bookingId: 'booking_1001',
      method: 'cash_on_arrival'
    });

    const res = await makeRequest('/api/payments/initiate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.method, 'cash_on_arrival');
    assert.equal(res.json.data.status, 'due_on_arrival');
  });

  test('POST /api/payments/initiate for unconfirmed booking returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      bookingId: 'booking_unconfirmed_999',
      method: 'bank_transfer'
    });

    const res = await makeRequest('/api/payments/initiate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123',
        'x-test-mock-unconfirmed': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('POST /api/payments/initiate with invalid payment method returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      bookingId: 'booking_1001',
      method: 'unsupported_credit_card'
    });

    const res = await makeRequest('/api/payments/initiate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('GET /api/payments/mine returns traveler payment records list', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/payments/mine', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.ok(Array.isArray(res.json.data));
  });

  test('GET /api/payments/:paymentId returns payment details for authorized user', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/payments/booking_1001', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.paymentId, 'booking_1001');
  });

  test('POST /api/payments/:paymentId/receipts uploads receipt proof and transitions status to under_review', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({
      mimeType: 'image/jpeg',
      fileBase64: '/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP123456789012345678901234567890'
    });

    const res = await makeRequest('/api/payments/booking_1001/receipts', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.paymentStatus, 'under_review');
    assert.ok(res.json.data.receiptId);
  });

  test('GET /api/payments/:paymentId/receipts/:receiptId/preview returns short-lived preview link', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/payments/booking_1001/receipts/receipt_123/preview', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'traveler_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.receiptId, 'receipt_123');
    assert.ok(res.json.data.authorizedPreviewUrl);
  });

  // --- SECURE ADMIN PAYMENT REVIEW & RECONCILIATION TESTS (PHASE 05-B3-1) ---

  test('GET /api/admin/payments without admin claims returns 403 FORBIDDEN', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/payments', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'ordinary_user_123'
      }
    });

    assert.equal(res.status, 403);
    assert.equal(res.json.code, 'FORBIDDEN');
  });

  test('GET /api/admin/payments with admin claims returns 200 OK', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/payments', {
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

  test('GET /api/admin/payments/:paymentId with admin claims returns payment detail with receipt metadata', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/payments/booking_1001', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.payment.paymentId, 'booking_1001');
    assert.ok(Array.isArray(res.json.data.receipts));
  });

  test('POST /api/admin/payments/:paymentId/decision verify decision updates status to verified', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'verify', bankReference: 'BANK_TX_98765' });

    const res = await makeRequest('/api/admin/payments/booking_1001/decision', {
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

  test('POST /api/admin/payments/:paymentId/decision reject decision without reason returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'reject' });

    const res = await makeRequest('/api/admin/payments/booking_1001/decision', {
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

  test('POST /api/admin/payments/:paymentId/decision reject decision with valid reason returns 200 OK', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ decision: 'reject', reason: 'Receipt photo was unreadable or amount mismatched.' });

    const res = await makeRequest('/api/admin/payments/booking_1001/decision', {
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

  // --- ROOM AVAILABILITY CALENDAR TESTS (PHASE 04-C3-1) ---

  test('GET /api/provider/listings/:listingId/rooms/:roomId/availability returns blocked days list', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/provider/listings/listing_123/rooms/room_101/availability', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.totalUnits, 2);
    assert.ok(Array.isArray(res.json.data.availabilityDays));
  });

  test('PUT /api/provider/listings/:listingId/rooms/:roomId/availability/:date sets blockedUnits = 0, 1, 2', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ blockedUnits: 1, note: 'One unit maintenance' });

    const res = await makeRequest('/api/provider/listings/listing_123/rooms/room_101/availability/2026-11-02', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123'
      },
      body: payload
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.blockedUnits, 1);
  });

  test('PUT /api/provider/listings/:listingId/rooms/:roomId/availability/:date exceeding capacity returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ blockedUnits: 3, note: 'Exceeds capacity' });

    const res = await makeRequest('/api/provider/listings/listing_123/rooms/room_101/availability/2026-11-04', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'provider_user_123',
        'x-test-mock-exceed-units': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
  });

  test('PUT /api/provider/listings/:listingId/rooms/:roomId/availability/:date negative blockedUnits returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ blockedUnits: -1, note: 'Negative' });

    const res = await makeRequest('/api/provider/listings/listing_123/rooms/room_101/availability/2026-11-05', {
      method: 'PUT',
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

  test('GET /api/admin/listings/:listingId with admin claims returns 200 OK with listing and provider profile details', async () => {
    process.env.NODE_ENV = 'test';
    const res = await makeRequest('/api/admin/listings/listing_123', {
      headers: {
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true'
      }
    });

    assert.equal(res.status, 200);
    assert.equal(res.json.status, 'success');
    assert.equal(res.json.data.listing.id, 'listing_123');
    assert.ok(res.json.data.providerProfile);
  });

  test('POST /api/admin/listings/:listingId/decision with provider UID spoofing attempt returns 400 BAD_REQUEST', async () => {
    process.env.NODE_ENV = 'test';
    const payload = JSON.stringify({ providerUid: 'spoofed_provider_999', decision: 'approve' });

    const res = await makeRequest('/api/admin/listings/listing_123/decision', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-test-mock-auth': 'true',
        'x-test-mock-uid': 'admin_user_999',
        'x-test-mock-admin': 'true',
        'x-test-mock-spoof-uid': 'true'
      },
      body: payload
    });

    assert.equal(res.status, 400);
    assert.equal(res.json.code, 'BAD_REQUEST');
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