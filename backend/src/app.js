'use strict';

const express = require('express');
const helmet = require('helmet');
const admin = require('firebase-admin');
const { requireFirebaseUser, requireAdmin, requireRole, initAdmin } = require('./auth');

const app = express();

app.disable('x-powered-by');
app.use(helmet());
app.use(express.json({ limit: '64kb' }));

// Health Check Endpoint (Public)
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'falconplan',
    scope: 'starter-only',
    timestamp: new Date().toISOString()
  });
});

// Authenticated User Identity Endpoint
app.get('/api/me', requireFirebaseUser, (req, res) => {
  res.json({
    status: 'success',
    data: {
      uid: req.auth.uid,
      email: req.auth.claims.email || null,
      emailVerified: req.auth.claims.email_verified || false,
      activeMode: req.auth.claims.activeMode || 'traveler'
    }
  });
});

// POST /api/plans/:planId/publish - Publish a private plan securely
app.post('/api/plans/:planId/publish', requireFirebaseUser, async (req, res) => {
  const planId = req.params.planId;
  const { title, description, startDate, endDate, stops, ownerDisplayName } = req.body || {};

  if (!planId || !planId.trim()) {
    return res.status(400).json({
      error: 'Invalid or missing planId parameter',
      code: 'BAD_REQUEST'
    });
  }

  const callerUid = req.auth.uid;

  // Sanitized Stop Processing
  const sanitizedStops = Array.isArray(stops)
    ? stops.map((s, idx) => ({
        destinationId: String(s.destinationId || '').trim(),
        destinationName: String(s.destinationName || '').trim(),
        district: String(s.district || '').trim(),
        latitude: typeof s.latitude === 'number' ? s.latitude : 0,
        longitude: typeof s.longitude === 'number' ? s.longitude : 0,
        orderIndex: idx + 1
      }))
    : [];

  const publicPlan = {
    publicPlanId: planId,
    ownerUid: callerUid,
    ownerDisplayName: String(ownerDisplayName || req.auth.claims.name || 'FalconPlan Member').slice(0, 80),
    title: String(title || 'Untitled Travel Plan').slice(0, 100),
    description: String(description || '').slice(0, 500),
    startDate: typeof startDate === 'number' ? startDate : Date.now(),
    endDate: typeof endDate === 'number' ? endDate : Date.now(),
    stops: sanitizedStops,
    publishedAt: Date.now(),
    status: 'published'
  };

  // If live Admin SDK is initialized, perform Firestore transactions
  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const privateDocRef = db.collection('users').doc(callerUid).collection('travelPlans').doc(planId);
      const publicDocRef = db.collection('publicTravelPlans').doc(planId);

      const privateSnap = await privateDocRef.get();
      if (!privateSnap.exists) {
        return res.status(404).json({
          error: 'Private plan not found or ownership mismatch',
          code: 'NOT_FOUND'
        });
      }

      const batch = db.batch();
      batch.set(publicDocRef, publicPlan);
      batch.update(privateDocRef, { status: 'published', updatedAt: Date.now() });
      await batch.commit();

      return res.json({
        status: 'success',
        data: publicPlan
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Firestore publishing failed: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Test / Mock mode response
  return res.json({
    status: 'success',
    data: publicPlan
  });
});

// POST /api/plans/:planId/unpublish - Unpublish a published plan
app.post('/api/plans/:planId/unpublish', requireFirebaseUser, async (req, res) => {
  const planId = req.params.planId;
  const callerUid = req.auth.uid;
  const isAdminUser = req.auth.claims && req.auth.claims.admin === true;

  if (!planId || !planId.trim()) {
    return res.status(400).json({
      error: 'Invalid or missing planId parameter',
      code: 'BAD_REQUEST'
    });
  }

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const publicDocRef = db.collection('publicTravelPlans').doc(planId);
      const publicSnap = await publicDocRef.get();

      if (!publicSnap.exists) {
        return res.status(404).json({
          error: 'Public plan not found',
          code: 'NOT_FOUND'
        });
      }

      const publicData = publicSnap.data();
      if (publicData.ownerUid !== callerUid && !isAdminUser) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this published plan',
          code: 'FORBIDDEN'
        });
      }

      const batch = db.batch();
      batch.delete(publicDocRef);

      const privateDocRef = db.collection('users').doc(callerUid).collection('travelPlans').doc(planId);
      batch.update(privateDocRef, { status: 'draft', updatedAt: Date.now() });

      await batch.commit();

      return res.json({
        status: 'success',
        message: 'Plan unpublished successfully',
        planId
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Firestore unpublishing failed: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Test / Mock mode response
  return res.json({
    status: 'success',
    message: 'Plan unpublished successfully',
    planId
  });
});

// GET /api/public-plans - Paginated list of public travel plans
app.get('/api/public-plans', async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collection('publicTravelPlans')
        .orderBy('publishedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const publicPlans = [];
      snapshot.forEach(doc => {
        publicPlans.push(doc.data());
      });

      return res.json({
        status: 'success',
        page,
        limit,
        total: publicPlans.length,
        data: publicPlans
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve public plans: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Test / Mock mode response
  return res.json({
    status: 'success',
    page,
    limit,
    total: 0,
    data: []
  });
});

// GET /api/public-plans/:publicPlanId - Single public plan detail
app.get('/api/public-plans/:publicPlanId', async (req, res) => {
  const publicPlanId = req.params.publicPlanId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const doc = await db.collection('publicTravelPlans').doc(publicPlanId).get();

      if (!doc.exists) {
        return res.status(404).json({
          error: 'Public plan not found',
          code: 'NOT_FOUND'
        });
      }

      return res.json({
        status: 'success',
        data: doc.data()
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch public plan: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.status(404).json({
    error: 'Public plan not found in mock test mode',
    code: 'NOT_FOUND'
  });
});

// Future Provider Workspace API Contract Placeholders
app.get('/api/provider/listings', requireFirebaseUser, requireRole(['provider']), (req, res) => {
  res.status(501).json({
    error: 'Provider listings endpoint contract placeholder. Business logic active in future phase.',
    code: 'NOT_IMPLEMENTED'
  });
});

// Future Admin Workspace API Contract Placeholders
app.get('/api/admin/overview', requireFirebaseUser, requireAdmin, (req, res) => {
  res.status(501).json({
    error: 'Administrator workspace contract placeholder.',
    code: 'NOT_IMPLEMENTED'
  });
});

// Catch-all 404 for unhandled API routes
app.use('/api', (req, res) => {
  res.status(404).json({
    error: 'Endpoint not implemented in FalconPlan starter API',
    code: 'NOT_FOUND'
  });
});

// Global Error Handler
app.use((err, req, res, _next) => {
  if (err && err.type === 'entity.too.large') {
    return res.status(413).json({
      error: 'Request body exceeds 64kb limit',
      code: 'PAYLOAD_TOO_LARGE'
    });
  }
  if (err instanceof SyntaxError && 'body' in err) {
    return res.status(400).json({
      error: 'Invalid JSON payload structure',
      code: 'BAD_REQUEST'
    });
  }
  return res.status(500).json({
    error: 'Internal server error',
    code: 'INTERNAL_ERROR'
  });
});

module.exports = app;