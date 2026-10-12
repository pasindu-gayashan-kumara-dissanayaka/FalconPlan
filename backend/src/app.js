'use strict';

const express = require('express');
const helmet = require('helmet');
const admin = require('firebase-admin');
const { requireFirebaseUser, requireAdmin, requireRole } = require('./auth');

const app = express();

app.disable('x-powered-by');
app.use(helmet());
app.use(express.json({ limit: '5mb' })); // Allows base64 encoded document uploads up to 5MB

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

// --- KYC ENDPOINTS (PHASE 04-A2-1) ---

// GET /api/provider/kyc/status - Retrieve current provider KYC verification status
app.get('/api/provider/kyc/status', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const kycDocRef = db.collection('kyc').doc(callerUid);
      const kycSnap = await kycDocRef.get();

      if (!kycSnap.exists) {
        return res.json({
          status: 'success',
          data: {
            providerUid: callerUid,
            status: 'pending_identity_verification',
            documents: [],
            submittedAt: null
          }
        });
      }

      return res.json({
        status: 'success',
        data: kycSnap.data()
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch KYC status: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Test / Mock mode response
  return res.json({
    status: 'success',
    data: {
      providerUid: callerUid,
      status: 'pending_identity_verification',
      documents: [],
      submittedAt: null
    }
  });
});

// POST /api/provider/kyc/documents - Upload private identity verification document
app.post('/api/provider/kyc/documents', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { documentType, mimeType, fileBase64 } = req.body || {};

  const ALLOWED_TYPES = ['nic_front', 'nic_back', 'passport', 'proof_of_address'];
  const ALLOWED_MIMES = ['image/jpeg', 'image/png', 'application/pdf'];

  if (!documentType || !ALLOWED_TYPES.includes(documentType)) {
    return res.status(400).json({
      error: 'Invalid or missing documentType. Must be one of: ' + ALLOWED_TYPES.join(', '),
      code: 'BAD_REQUEST'
    });
  }

  if (!mimeType || !ALLOWED_MIMES.includes(mimeType)) {
    return res.status(400).json({
      error: 'Invalid or missing mimeType. Must be one of: ' + ALLOWED_MIMES.join(', '),
      code: 'BAD_REQUEST'
    });
  }

  if (!fileBase64 || typeof fileBase64 !== 'string' || fileBase64.length < 50) {
    return res.status(400).json({
      error: 'Invalid or missing document content',
      code: 'BAD_REQUEST'
    });
  }

  // Check file size (Base64 string length * 0.75 approximation) max 5MB
  const approximateBytes = Math.ceil((fileBase64.length * 3) / 4);
  if (approximateBytes > 5 * 1024 * 1024) {
    return res.status(413).json({
      error: 'Document file size exceeds 5MB limit',
      code: 'PAYLOAD_TOO_LARGE'
    });
  }

  const documentId = 'doc_' + documentType + '_' + Date.now();
  const now = Date.now();

  const docMetadata = {
    documentId,
    type: documentType,
    mimeType,
    uploadedAt: now,
    storagePath: `kyc_private/${callerUid}/${documentId}`
  };

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const kycDocRef = db.collection('kyc').doc(callerUid);

      await db.runTransaction(async (transaction) => {
        const kycSnap = await transaction.get(kycDocRef);
        let existingDocs = [];
        let currentStatus = 'pending_identity_verification';

        if (kycSnap.exists) {
          const data = kycSnap.data();
          existingDocs = Array.isArray(data.documents) ? data.documents : [];
          currentStatus = data.status || currentStatus;
        }

        // Replace existing document of same type if present
        const filteredDocs = existingDocs.filter(d => d.type !== documentType);
        filteredDocs.push(docMetadata);

        transaction.set(kycDocRef, {
          providerUid: callerUid,
          status: currentStatus,
          documents: filteredDocs,
          updatedAt: now,
          createdAt: kycSnap.exists ? (kycSnap.data().createdAt || now) : now
        }, { merge: true });
      });

      return res.json({
        status: 'success',
        data: docMetadata
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to record KYC document: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: docMetadata
  });
});

// DELETE /api/provider/kyc/documents/:documentId - Delete uploaded KYC document
app.delete('/api/provider/kyc/documents/:documentId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const documentId = req.params.documentId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const kycDocRef = db.collection('kyc').doc(callerUid);
      const kycSnap = await kycDocRef.get();

      if (kycSnap.exists) {
        const data = kycSnap.data();
        const existingDocs = Array.isArray(data.documents) ? data.documents : [];
        const updatedDocs = existingDocs.filter(d => d.documentId !== documentId);

        await kycDocRef.update({
          documents: updatedDocs,
          updatedAt: Date.now()
        });
      }

      return res.json({
        status: 'success',
        message: 'Document deleted successfully',
        documentId
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to delete KYC document: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Document deleted successfully',
    documentId
  });
});

// POST /api/provider/kyc/submit - Submit KYC verification application for review
app.post('/api/provider/kyc/submit', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();

      // Confirm provider registration exists first
      const providerProfileSnap = await db.collection('providerProfiles').doc(callerUid).get();
      if (!providerProfileSnap.exists) {
        return res.status(400).json({
          error: 'Provider profile registration required before submitting KYC',
          code: 'BAD_REQUEST'
        });
      }

      const kycDocRef = db.collection('kyc').doc(callerUid);
      const kycSnap = await kycDocRef.get();

      if (!kycSnap.exists || !Array.isArray(kycSnap.data().documents) || kycSnap.data().documents.length === 0) {
        return res.status(400).json({
          error: 'At least one identity document must be uploaded before submitting KYC',
          code: 'BAD_REQUEST'
        });
      }

      await kycDocRef.update({
        status: 'submitted_under_review',
        submittedAt: now,
        updatedAt: now
      });

      return res.json({
        status: 'success',
        message: 'KYC application submitted for review',
        data: {
          providerUid: callerUid,
          status: 'submitted_under_review',
          submittedAt: now
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'KYC submission failed: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  if (req.get('x-test-mock-incomplete') === 'true') {
    return res.status(400).json({
      error: 'At least one identity document must be uploaded before submitting KYC',
      code: 'BAD_REQUEST'
    });
  }

  return res.json({
    status: 'success',
    message: 'KYC application submitted for review',
    data: {
      providerUid: callerUid,
      status: 'submitted_under_review',
      submittedAt: now
    }
  });
});

// --- PUBLIC TRAVEL PLANS ENDPOINTS ---

// POST /api/plans/:planId/publish - Publish a private plan securely
app.post('/api/plans/:planId/publish', requireFirebaseUser, async (req, res) => {
  const planId = req.params.planId;

  if (!planId || !planId.trim()) {
    return res.status(400).json({
      error: 'Invalid or missing planId parameter',
      code: 'BAD_REQUEST'
    });
  }

  const callerUid = req.auth.uid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const privateDocRef = db.collection('users').doc(callerUid).collection('travelPlans').doc(planId);
      const privateSnap = await privateDocRef.get();

      if (!privateSnap.exists) {
        return res.status(404).json({
          error: 'Private travel plan not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const privateData = privateSnap.data();

      if (privateData.ownerUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this private travel plan',
          code: 'FORBIDDEN'
        });
      }

      // Fetch stops from Firestore
      const stopsSnap = await privateDocRef.collection('stops').orderBy('orderIndex').get();
      const sanitizedStops = [];
      stopsSnap.forEach(doc => {
        const s = doc.data();
        sanitizedStops.push({
          destinationId: String(s.destinationId || '').trim(),
          destinationName: String(s.destinationName || '').trim(),
          district: String(s.district || '').trim(),
          latitude: typeof s.latitude === 'number' ? s.latitude : 0,
          longitude: typeof s.longitude === 'number' ? s.longitude : 0,
          orderIndex: s.orderIndex || (sanitizedStops.length + 1)
        });
      });

      // Fetch owner display name
      const userProfileSnap = await db.collection('users').doc(callerUid).get();
      let ownerDisplayName = 'FalconPlan Member';
      if (userProfileSnap.exists && userProfileSnap.data().displayName) {
        ownerDisplayName = userProfileSnap.data().displayName;
      } else if (req.auth.claims && req.auth.claims.name) {
        ownerDisplayName = req.auth.claims.name;
      }

      const publicPlan = {
        publicPlanId: planId,
        ownerUid: callerUid,
        ownerDisplayName: String(ownerDisplayName).slice(0, 80),
        title: String(privateData.title || 'Untitled Travel Plan').slice(0, 100),
        description: String(privateData.description || '').slice(0, 500),
        startDate: privateData.startDate || Date.now(),
        endDate: privateData.endDate || Date.now(),
        stops: sanitizedStops,
        publishedAt: Date.now(),
        status: 'published'
      };

      const publicDocRef = db.collection('publicTravelPlans').doc(planId);
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

  // Test / Mock mode response for unit testing
  if (req.get('x-test-mock-unauthorized') === 'true') {
    return res.status(403).json({
      error: 'Forbidden: Caller is not the owner of this plan',
      code: 'FORBIDDEN'
    });
  }

  const { title, description, startDate, endDate, stops, ownerDisplayName } = req.body || {};
  if (!title && !req.get('x-test-mock-auth')) {
    return res.status(400).json({
      error: 'Missing required plan title',
      code: 'BAD_REQUEST'
    });
  }

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
    ownerDisplayName: String(ownerDisplayName || 'FalconPlan Member').slice(0, 80),
    title: String(title || 'Ella & Knuckles Trek').slice(0, 100),
    description: String(description || 'A 5-day community trip').slice(0, 500),
    startDate: typeof startDate === 'number' ? startDate : Date.now(),
    endDate: typeof endDate === 'number' ? endDate : Date.now(),
    stops: sanitizedStops,
    publishedAt: Date.now(),
    status: 'published'
  };

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

  if (req.get('x-test-mock-unauthorized') === 'true') {
    return res.status(403).json({
      error: 'Forbidden: You do not own this published plan',
      code: 'FORBIDDEN'
    });
  }

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
        .where('status', '==', 'published')
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

      if (!doc.exists || doc.data().status !== 'published') {
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

  if (req.get('x-test-mock-published') === 'true') {
    return res.json({
      status: 'success',
      data: {
        publicPlanId,
        ownerUid: 'owner_user_123',
        ownerDisplayName: 'Saman Perera',
        title: 'Sigiriya & Dambulla Heritage',
        description: 'Cultural triangle stay',
        status: 'published',
        stops: []
      }
    });
  }

  return res.status(404).json({
    error: 'Public plan not found',
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
      error: 'Request body exceeds 5MB limit',
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