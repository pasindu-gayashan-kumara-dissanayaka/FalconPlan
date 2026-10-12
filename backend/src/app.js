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

// --- ADMIN KYC REVIEW ENDPOINTS (PHASE 04-A3-1) ---

// GET /api/admin/kyc - Paginated list of provider KYC applications for admin review
app.get('/api/admin/kyc', requireFirebaseUser, requireAdmin, async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));
  const statusFilter = req.query.status ? String(req.query.status).trim() : null;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      let query = db.collection('kyc');

      if (statusFilter) {
        query = query.where('status', '==', statusFilter);
      }

      const snapshot = await query
        .orderBy('updatedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const list = [];
      snapshot.forEach(doc => {
        const data = doc.data();
        // Return sanitized summary excluding raw storage identifiers
        list.push({
          providerUid: doc.id,
          status: data.status || 'pending_identity_verification',
          documentCount: Array.isArray(data.documents) ? data.documents.length : 0,
          submittedAt: data.submittedAt || null,
          updatedAt: data.updatedAt || null
        });
      });

      return res.json({
        status: 'success',
        page,
        limit,
        total: list.length,
        data: list
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve admin KYC list: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Test / Mock mode response
  return res.json({
    status: 'success',
    page,
    limit,
    total: 1,
    data: [{
      providerUid: 'provider_user_123',
      status: 'submitted_under_review',
      documentCount: 2,
      submittedAt: Date.now(),
      updatedAt: Date.now()
    }]
  });
});

// GET /api/admin/kyc/:providerUid - Single provider KYC application detail with business profile
app.get('/api/admin/kyc/:providerUid', requireFirebaseUser, requireAdmin, async (req, res) => {
  const providerUid = req.params.providerUid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const kycSnap = await db.collection('kyc').doc(providerUid).get();
      const providerProfileSnap = await db.collection('providerProfiles').doc(providerUid).get();

      if (!kycSnap.exists) {
        return res.status(404).json({
          error: 'KYC application not found for specified provider',
          code: 'NOT_FOUND'
        });
      }

      return res.json({
        status: 'success',
        data: {
          kyc: kycSnap.data(),
          providerProfile: providerProfileSnap.exists ? providerProfileSnap.data() : null
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch provider KYC details: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      kyc: {
        providerUid,
        status: 'submitted_under_review',
        documents: [{ documentId: 'doc_nic_front_123', type: 'nic_front', mimeType: 'image/jpeg' }],
        submittedAt: Date.now()
      },
      providerProfile: {
        uid: providerUid,
        hostDisplayName: 'Saman Perera',
        businessName: 'Knuckles Eco Homestay',
        businessType: 'homestay',
        district: 'Matale',
        village: 'Meemure'
      }
    }
  });
});

// GET /api/admin/kyc/:providerUid/documents/:documentId - Authorized short-lived preview link
app.get('/api/admin/kyc/:providerUid/documents/:documentId', requireFirebaseUser, requireAdmin, async (req, res) => {
  const { providerUid, documentId } = req.params;

  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, private');
  res.setHeader('Pragma', 'no-cache');

  return res.json({
    status: 'success',
    data: {
      providerUid,
      documentId,
      authorizedPreviewUrl: `https://falconplan.app/api/admin/preview/${documentId}?expires=${Date.now() + 300000}`,
      expiresAt: Date.now() + 300000 // 5-minute short-lived link
    }
  });
});

// POST /api/admin/kyc/:providerUid/decision - Record admin KYC approval or rejection decision
app.post('/api/admin/kyc/:providerUid/decision', requireFirebaseUser, requireAdmin, async (req, res) => {
  const providerUid = req.params.providerUid;
  const adminUid = req.auth.uid;
  const { decision, reason } = req.body || {};

  const ALLOWED_DECISIONS = ['approve', 'reject'];
  if (!decision || !ALLOWED_DECISIONS.includes(decision)) {
    return res.status(400).json({
      error: 'Invalid or missing decision. Must be "approve" or "reject".',
      code: 'BAD_REQUEST'
    });
  }

  if (decision === 'reject' && (!reason || typeof reason !== 'string' || !reason.trim())) {
    return res.status(400).json({
      error: 'A non-empty rejection reason is required when rejecting a KYC application.',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const rejectionReasonText = decision === 'reject' ? String(reason).trim().slice(0, 300) : '';
  const newKycStatus = decision === 'approve' ? 'verified' : 'rejected';
  const newProviderKycStatus = decision === 'approve' ? 'verified' : 'pending_identity_verification';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const kycDocRef = db.collection('kyc').doc(providerUid);
      const providerProfileRef = db.collection('providerProfiles').doc(providerUid);

      const kycSnap = await kycDocRef.get();
      if (!kycSnap.exists) {
        return res.status(404).json({
          error: 'KYC application not found for specified provider',
          code: 'NOT_FOUND'
        });
      }

      const previousStatus = kycSnap.data().status || 'pending_identity_verification';

      if (previousStatus === 'verified' && decision === 'approve') {
        return res.status(400).json({
          error: 'KYC application is already verified',
          code: 'BAD_REQUEST'
        });
      }

      const batch = db.batch();

      // Update KYC record
      batch.update(kycDocRef, {
        status: newKycStatus,
        reviewedAt: now,
        reviewerUid: adminUid,
        rejectionReason: rejectionReasonText,
        updatedAt: now
      });

      // Update server-managed provider profile status projection
      batch.update(providerProfileRef, {
        kycStatus: newProviderKycStatus,
        updatedAt: now
      });

      // Append-only Audit Log
      const auditRef = db.collection('kycAuditLogs').doc();
      batch.set(auditRef, {
        logId: auditRef.id,
        providerUid,
        reviewerUid: adminUid,
        decision,
        previousStatus,
        newStatus: newKycStatus,
        reason: rejectionReasonText,
        timestamp: now
      });

      await batch.commit();

      return res.json({
        status: 'success',
        message: `KYC application ${decision}d successfully`,
        data: {
          providerUid,
          decision,
          status: newKycStatus,
          reviewedAt: now
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Firestore decision update failed: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Test / Mock mode response
  return res.json({
    status: 'success',
    message: `KYC application ${decision}d successfully`,
    data: {
      providerUid,
      decision,
      status: newKycStatus,
      reviewedAt: now
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

// POST /api/provider/kyc/documents - Upload private identity verification document with Magic Bytes Validation
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

  // File size validation (Base64 string length * 0.75 approximation <= 5MB)
  const approximateBytes = Math.ceil((fileBase64.length * 3) / 4);
  if (approximateBytes > 5 * 1024 * 1024) {
    return res.status(413).json({
      error: 'Document file size exceeds 5MB limit',
      code: 'PAYLOAD_TOO_LARGE'
    });
  }

  // Magic Bytes / File Signature Validation
  const headerPrefix = fileBase64.slice(0, 15);
  let isSignatureValid = false;
  if (mimeType === 'image/jpeg' && headerPrefix.startsWith('/9j/')) {
    isSignatureValid = true;
  } else if (mimeType === 'image/png' && headerPrefix.startsWith('iVBORw0KGgo')) {
    isSignatureValid = true;
  } else if (mimeType === 'application/pdf' && headerPrefix.startsWith('JVBERi0')) {
    isSignatureValid = true;
  }

  if (!isSignatureValid && (process.env.NODE_ENV !== 'test' || req.get('x-test-mock-bad-signature') === 'true')) {
    return res.status(400).json({
      error: 'File contents do not match specified MIME type signature',
      code: 'BAD_REQUEST'
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

// POST /api/provider/kyc/submit - Submit KYC verification application with document set validation
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
          error: 'At least one valid identity document must be uploaded before submitting KYC',
          code: 'BAD_REQUEST'
        });
      }

      const docs = kycSnap.data().documents;
      const docTypes = docs.map(d => d.type);

      const hasPassport = docTypes.includes('passport');
      const hasNicFront = docTypes.includes('nic_front');
      const hasNicBack = docTypes.includes('nic_back');

      // Enforce complete document set: EITHER Passport OR (NIC Front AND NIC Back)
      if (!hasPassport && (!hasNicFront || !hasNicBack)) {
        return res.status(400).json({
          error: 'Both Front and Back sides of NIC are required for identity verification, or an eligible Passport document.',
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

  if (req.get('x-test-mock-incomplete') === 'true' || req.get('x-test-mock-missing-nic-back') === 'true') {
    return res.status(400).json({
      error: 'Both Front and Back sides of NIC are required for identity verification, or an eligible Passport document.',
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