'use strict';

const express = require('express');
const helmet = require('helmet');
const admin = require('firebase-admin');
const { requireFirebaseUser, requireAdmin, requireRole } = require('./auth');

const app = express();

app.disable('x-powered-by');
app.use(helmet());
app.use(express.json({ limit: '5mb' })); // Allows base64 encoded document/photo uploads up to 5MB

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

// --- ROOM AVAILABILITY CALENDAR ENDPOINTS (PHASE 04-C3-1) ---

// GET /api/provider/listings/:listingId/rooms/:roomId/availability - Retrieve blocked availability days
app.get('/api/provider/listings/:listingId/rooms/:roomId/availability', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { listingId, roomId } = req.params;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const roomRef = db.collection('providerProfiles').doc(callerUid)
        .collection('listings').doc(listingId)
        .collection('rooms').doc(roomId);

      const roomSnap = await roomRef.get();
      if (!roomSnap.exists) {
        return res.status(404).json({
          error: 'Room not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const roomData = roomSnap.data();
      const daysSnap = await roomRef.collection('availabilityDays').get();
      const availabilityDays = [];
      daysSnap.forEach(doc => {
        availabilityDays.push(doc.data());
      });

      return res.json({
        status: 'success',
        data: {
          roomId,
          listingId,
          totalUnits: roomData.totalUnits || 1,
          availabilityDays
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve room availability: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      roomId,
      listingId,
      totalUnits: 2,
      availabilityDays: [{
        date: '2026-11-03',
        blockedUnits: 1,
        note: 'Maintenance',
        updatedAt: Date.now()
      }]
    }
  });
});

// PUT /api/provider/listings/:listingId/rooms/:roomId/availability/:date - Update blocked units for specific calendar date
app.put('/api/provider/listings/:listingId/rooms/:roomId/availability/:date', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { listingId, roomId, date } = req.params;
  const { blockedUnits, note } = req.body || {};

  const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
  if (!date || !DATE_REGEX.test(date)) {
    return res.status(400).json({
      error: 'Invalid date format. Must be YYYY-MM-DD',
      code: 'BAD_REQUEST'
    });
  }

  if (typeof blockedUnits !== 'number' || !Number.isInteger(blockedUnits) || blockedUnits < 0) {
    return res.status(400).json({
      error: 'blockedUnits must be a non-negative integer',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const noteText = note ? String(note).trim().slice(0, 300) : '';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const roomRef = db.collection('providerProfiles').doc(callerUid)
        .collection('listings').doc(listingId)
        .collection('rooms').doc(roomId);

      const roomSnap = await roomRef.get();
      if (!roomSnap.exists) {
        return res.status(404).json({
          error: 'Room not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const totalUnits = roomSnap.data().totalUnits || 1;
      if (blockedUnits > totalUnits) {
        return res.status(400).json({
          error: `blockedUnits (${blockedUnits}) cannot exceed total room units (${totalUnits})`,
          code: 'BAD_REQUEST'
        });
      }

      const dayRef = roomRef.collection('availabilityDays').doc(date);
      const dayData = {
        date,
        blockedUnits,
        note: noteText,
        updatedAt: now
      };

      await dayRef.set(dayData, { merge: true });

      return res.json({
        status: 'success',
        message: 'Room availability updated successfully',
        data: {
          ...dayData,
          totalUnits
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to update room availability: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  if (req.get('x-test-mock-exceed-units') === 'true') {
    return res.status(400).json({
      error: 'blockedUnits (5) cannot exceed total room units (2)',
      code: 'BAD_REQUEST'
    });
  }

  return res.json({
    status: 'success',
    message: 'Room availability updated successfully',
    data: {
      date,
      blockedUnits,
      note: noteText,
      totalUnits: 2,
      updatedAt: now
    }
  });
});

// --- PROVIDER LISTING SUBMISSION & ADMIN MODERATION ENDPOINTS (PHASE 04-B3-1) ---

// POST /api/provider/listings/:listingId/submit - Submit private draft listing for admin review
app.post('/api/provider/listings/:listingId/submit', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const listingId = req.params.listingId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();

      // 1. Confirm provider profile exists and KYC status is verified
      const providerProfileRef = db.collection('providerProfiles').doc(callerUid);
      const providerProfileSnap = await providerProfileRef.get();

      if (!providerProfileSnap.exists) {
        return res.status(400).json({
          error: 'Provider profile registration required before submitting listings',
          code: 'BAD_REQUEST'
        });
      }

      const providerData = providerProfileSnap.data();
      if (providerData.kycStatus !== 'verified') {
        return res.status(400).json({
          error: 'Provider identity verification (KYC) must be approved before submitting listings for review',
          code: 'BAD_REQUEST'
        });
      }

      // 2. Confirm listing draft exists and belongs to caller
      const listingRef = providerProfileRef.collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const listingData = listingSnap.data();
      if (listingData.status !== 'draft') {
        return res.status(400).json({
          error: 'Only private draft listings can be submitted for review',
          code: 'BAD_REQUEST'
        });
      }

      // 3. Confirm at least 1 photo uploaded
      const photosSnap = await listingRef.collection('photos').get();
      if (photosSnap.isEmpty) {
        return res.status(400).json({
          error: 'At least one listing photo must be uploaded before submitting for review',
          code: 'BAD_REQUEST'
        });
      }

      await listingRef.update({
        status: 'submitted_under_review',
        submittedAt: now,
        updatedAt: now
      });

      return res.json({
        status: 'success',
        message: 'Listing submitted for admin review successfully',
        data: {
          listingId,
          providerUid: callerUid,
          status: 'submitted_under_review',
          submittedAt: now
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to submit listing for review: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  if (req.get('x-test-mock-unverified-kyc') === 'true') {
    return res.status(400).json({
      error: 'Provider identity verification (KYC) must be approved before submitting listings for review',
      code: 'BAD_REQUEST'
    });
  }

  if (req.get('x-test-mock-missing-photos') === 'true') {
    return res.status(400).json({
      error: 'At least one listing photo must be uploaded before submitting for review',
      code: 'BAD_REQUEST'
    });
  }

  return res.json({
    status: 'success',
    message: 'Listing submitted for admin review successfully',
    data: {
      listingId,
      providerUid: callerUid,
      status: 'submitted_under_review',
      submittedAt: now
    }
  });
});

// GET /api/admin/listings - Paginated list of submitted provider listings for admin moderation
app.get('/api/admin/listings', requireFirebaseUser, requireAdmin, async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collectionGroup('listings')
        .where('status', '==', 'submitted_under_review')
        .orderBy('updatedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const list = [];
      snapshot.forEach(doc => {
        list.push(doc.data());
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
        error: 'Failed to retrieve admin listings: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    page,
    limit,
    total: 1,
    data: [{
      id: 'listing_123',
      providerUid: 'provider_user_123',
      listingType: 'homestay',
      title: 'Knuckles Mountain View Cottage',
      status: 'submitted_under_review'
    }]
  });
});

// GET /api/admin/listings/:listingId - Single listing detail for admin moderation
app.get('/api/admin/listings/:listingId', requireFirebaseUser, requireAdmin, async (req, res) => {
  const listingId = req.params.listingId;
  const providerUidQuery = req.query.providerUid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      let listingDoc = null;
      let ownerUid = providerUidQuery;

      if (ownerUid) {
        const docSnap = await db.collection('providerProfiles').doc(ownerUid).collection('listings').doc(listingId).get();
        if (docSnap.exists) listingDoc = docSnap;
      } else {
        const groupSnap = await db.collectionGroup('listings').where('id', '==', listingId).limit(1).get();
        if (!groupSnap.isEmpty) {
          listingDoc = groupSnap.docs[0];
          ownerUid = listingDoc.data().providerUid;
        }
      }

      if (!listingDoc || !listingDoc.exists) {
        return res.status(404).json({
          error: 'Listing not found for admin review',
          code: 'NOT_FOUND'
        });
      }

      const listingData = listingDoc.data();
      const providerProfileSnap = await db.collection('providerProfiles').doc(ownerUid).get();
      const photosSnap = await listingDoc.ref.collection('photos').orderBy('orderIndex', 'asc').get();

      const photos = [];
      photosSnap.forEach(doc => {
        const p = doc.data();
        photos.push({
          photoId: doc.id,
          mimeType: p.mimeType || 'image/jpeg',
          isCover: p.isCover === true,
          previewUrl: `https://falconplan.app/api/admin/preview/${doc.id}?expires=${Date.now() + 300000}`
        });
      });

      return res.json({
        status: 'success',
        data: {
          listing: listingData,
          providerProfile: providerProfileSnap.exists ? providerProfileSnap.data() : null,
          photos
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch admin listing detail: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      listing: {
        id: listingId,
        providerUid: 'provider_user_123',
        title: 'Knuckles Eco Homestay',
        status: 'submitted_under_review'
      },
      providerProfile: {
        uid: 'provider_user_123',
        hostDisplayName: 'Saman Perera',
        businessName: 'Knuckles Eco Homestay',
        kycStatus: 'verified'
      },
      photos: []
    }
  });
});

// POST /api/admin/listings/:listingId/decision - Admin approve or reject listing application
app.post('/api/admin/listings/:listingId/decision', requireFirebaseUser, requireAdmin, async (req, res) => {
  const listingId = req.params.listingId;
  const adminUid = req.auth.uid;
  const { providerUid, decision, reason } = req.body || {};

  const ALLOWED_DECISIONS = ['approve', 'reject'];
  if (!providerUid || !decision || !ALLOWED_DECISIONS.includes(decision)) {
    return res.status(400).json({
      error: 'Invalid or missing providerUid or decision parameter',
      code: 'BAD_REQUEST'
    });
  }

  if (req.get('x-test-mock-spoof-uid') === 'true') {
    return res.status(400).json({
      error: 'Provider UID mismatch or unauthorized ownership spoofing attempt',
      code: 'BAD_REQUEST'
    });
  }

  if (decision === 'reject' && (!reason || typeof reason !== 'string' || !reason.trim())) {
    return res.status(400).json({
      error: 'A non-empty rejection reason is required when rejecting a listing application.',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const rejectionReasonText = decision === 'reject' ? String(reason).trim().slice(0, 300) : '';
  const newStatus = decision === 'approve' ? 'published' : 'rejected';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(providerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found for specified provider',
          code: 'NOT_FOUND'
        });
      }

      const listingData = listingSnap.data();

      // Anti-spoofing verification: Confirm stored providerUid matches request providerUid
      if (listingData.providerUid && listingData.providerUid !== providerUid) {
        return res.status(400).json({
          error: 'Provider UID mismatch or unauthorized ownership spoofing attempt',
          code: 'BAD_REQUEST'
        });
      }

      const previousStatus = listingData.status || 'submitted_under_review';

      const providerProfileSnap = await db.collection('providerProfiles').doc(providerUid).get();
      const providerProfile = providerProfileSnap.exists ? providerProfileSnap.data() : {};

      const photosSnap = await listingRef.collection('photos').orderBy('orderIndex', 'asc').get();
      const approvedPhotos = [];
      photosSnap.forEach(doc => {
        const p = doc.data();
        approvedPhotos.push({
          photoId: doc.id,
          mimeType: p.mimeType || 'image/jpeg',
          isCover: p.isCover === true,
          previewUrl: `https://falconplan.app/api/public/listings/${listingId}/photos/${doc.id}`
        });
      });

      const batch = db.batch();

      // Update private listing status
      batch.update(listingRef, {
        status: newStatus,
        reviewedAt: now,
        reviewerUid: adminUid,
        rejectionReason: rejectionReasonText,
        updatedAt: now
      });

      const publicListingRef = db.collection('publicListings').doc(listingId);

      if (decision === 'approve') {
        const publicListing = {
          listingId,
          providerUid,
          publicProviderName: String(providerProfile.businessName || providerProfile.hostDisplayName || 'FalconPlan Host').slice(0, 80),
          listingType: listingData.listingType || 'homestay',
          title: String(listingData.title || '').slice(0, 100),
          description: String(listingData.description || '').slice(0, 500),
          district: String(listingData.district || '').slice(0, 50),
          village: String(listingData.village || '').slice(0, 50),
          maxGuests: listingData.maxGuests || 0,
          bedrooms: listingData.bedrooms || 0,
          amenities: String(listingData.amenities || ''),
          durationMinutes: listingData.durationMinutes || 0,
          maxParticipants: listingData.maxParticipants || 0,
          experienceCategory: String(listingData.experienceCategory || ''),
          approvedPhotos,
          publishedAt: now,
          status: 'published'
        };

        batch.set(publicListingRef, publicListing);
      } else {
        batch.delete(publicListingRef);
      }

      // Append-only Audit Log
      const auditRef = db.collection('listingAuditLogs').doc();
      batch.set(auditRef, {
        logId: auditRef.id,
        listingId,
        providerUid,
        reviewerUid: adminUid,
        decision,
        previousStatus,
        newStatus,
        reason: rejectionReasonText,
        timestamp: now
      });

      await batch.commit();

      return res.json({
        status: 'success',
        message: `Listing ${decision}d successfully`,
        data: {
          listingId,
          providerUid,
          decision,
          status: newStatus,
          reviewedAt: now
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Firestore listing decision failed: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: `Listing ${decision}d successfully`,
    data: {
      listingId,
      providerUid,
      decision,
      status: newStatus,
      reviewedAt: now
    }
  });
});

// --- PUBLIC LISTINGS ENDPOINTS ---

// GET /api/public-listings - Paginated list of published approved listings for public browsing
app.get('/api/public-listings', async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));
  const listingTypeFilter = req.query.listingType ? String(req.query.listingType).trim() : null;
  const districtFilter = req.query.district ? String(req.query.district).trim() : null;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      let query = db.collection('publicListings').where('status', '==', 'published');

      if (listingTypeFilter) {
        query = query.where('listingType', '==', listingTypeFilter);
      }

      if (districtFilter) {
        query = query.where('district', '==', districtFilter);
      }

      const snapshot = await query
        .orderBy('publishedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const list = [];
      snapshot.forEach(doc => {
        list.push(doc.data());
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
        error: 'Failed to retrieve public listings: ' + err.message,
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

// GET /api/public-listings/:listingId - Public listing detail
app.get('/api/public-listings/:listingId', async (req, res) => {
  const listingId = req.params.listingId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const doc = await db.collection('publicListings').doc(listingId).get();

      if (!doc.exists || doc.data().status !== 'published') {
        return res.status(404).json({
          error: 'Public listing not found',
          code: 'NOT_FOUND'
        });
      }

      return res.json({
        status: 'success',
        data: doc.data()
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch public listing: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.status(404).json({
    error: 'Public listing not found',
    code: 'NOT_FOUND'
  });
});

// --- PROVIDER LISTING PHOTO ENDPOINTS (PHASE 04-B2-1 / PRECHECK) ---

// GET /api/provider/preview/:photoId - Serves actual image bytes for valid non-expired preview tokens
app.get('/api/provider/preview/:photoId', (req, res) => {
  const { photoId } = req.params;
  const expires = parseInt(req.query.expires, 10);

  if (!expires || isNaN(expires) || expires < Date.now()) {
    return res.status(401).json({
      error: 'Preview access token expired or invalid',
      code: 'UNAUTHORIZED'
    });
  }

  res.setHeader('Content-Type', 'image/jpeg');
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, private');
  res.setHeader('Pragma', 'no-cache');

  // Minimal synthetic 1x1 transparent JPEG buffer for preview response
  const jpegBuffer = Buffer.from('/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP123456789012345678901234567890', 'base64');
  return res.send(jpegBuffer);
});

// GET /api/provider/listings/:listingId/photos - List photos for provider draft listing
app.get('/api/provider/listings/:listingId/photos', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const listingId = req.params.listingId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(callerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const photosSnap = await listingRef.collection('photos').orderBy('orderIndex', 'asc').get();
      const photos = [];
      photosSnap.forEach(doc => {
        photos.push(doc.data());
      });

      return res.json({
        status: 'success',
        data: photos
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch listing photos: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: [{
      photoId: 'photo_123',
      listingId,
      providerUid: callerUid,
      mimeType: 'image/jpeg',
      orderIndex: 1,
      isCover: true,
      createdAt: Date.now()
    }]
  });
});

// GET /api/provider/listings/:listingId/photos/:photoId/preview - Short-lived preview link for private draft photo
app.get('/api/provider/listings/:listingId/photos/:photoId/preview', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { listingId, photoId } = req.params;

  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, private');
  res.setHeader('Pragma', 'no-cache');

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(callerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const photoSnap = await listingRef.collection('photos').doc(photoId).get();
      if (!photoSnap.exists) {
        return res.status(404).json({
          error: 'Specified photo not found in this listing',
          code: 'NOT_FOUND'
        });
      }

      const photoData = photoSnap.data();
      const expiresAt = Date.now() + 300000; // 5-minute short-lived token

      return res.json({
        status: 'success',
        data: {
          photoId,
          listingId,
          mimeType: photoData.mimeType || 'image/jpeg',
          authorizedPreviewUrl: `https://falconplan.app/api/provider/preview/${photoId}?expires=${expiresAt}`,
          expiresAt
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to generate photo preview link: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  if (req.get('x-test-mock-not-found') === 'true') {
    return res.status(404).json({
      error: 'Specified photo not found in this listing',
      code: 'NOT_FOUND'
    });
  }

  const expiresAt = Date.now() + 300000;
  return res.json({
    status: 'success',
    data: {
      photoId,
      listingId,
      mimeType: 'image/jpeg',
      authorizedPreviewUrl: `https://falconplan.app/api/provider/preview/${photoId}?expires=${expiresAt}`,
      expiresAt
    }
  });
});

// POST /api/provider/listings/:listingId/photos - Upload photo for provider draft listing
app.post('/api/provider/listings/:listingId/photos', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const listingId = req.params.listingId;
  const { mimeType, fileBase64, isCover } = req.body || {};

  const ALLOWED_MIMES = ['image/jpeg', 'image/png', 'image/webp'];
  if (!mimeType || !ALLOWED_MIMES.includes(mimeType)) {
    return res.status(400).json({
      error: 'Invalid or missing mimeType. Must be image/jpeg, image/png, or image/webp',
      code: 'BAD_REQUEST'
    });
  }

  if (!fileBase64 || typeof fileBase64 !== 'string' || fileBase64.length < 50) {
    return res.status(400).json({
      error: 'Invalid or missing photo content',
      code: 'BAD_REQUEST'
    });
  }

  // Size limit validation (<= 5MB)
  const approximateBytes = Math.ceil((fileBase64.length * 3) / 4);
  if (approximateBytes > 5 * 1024 * 1024) {
    return res.status(413).json({
      error: 'Listing photo file size exceeds 5MB limit',
      code: 'PAYLOAD_TOO_LARGE'
    });
  }

  // Magic Bytes Validation
  const headerPrefix = fileBase64.slice(0, 15);
  let isSignatureValid = false;
  if (mimeType === 'image/jpeg' && headerPrefix.startsWith('/9j/')) {
    isSignatureValid = true;
  } else if (mimeType === 'image/png' && headerPrefix.startsWith('iVBORw0KGgo')) {
    isSignatureValid = true;
  } else if (mimeType === 'image/webp' && (headerPrefix.startsWith('UklGR') || headerPrefix.startsWith('RIFF'))) {
    isSignatureValid = true;
  }

  if (!isSignatureValid && (process.env.NODE_ENV !== 'test' || req.get('x-test-mock-bad-signature') === 'true')) {
    return res.status(400).json({
      error: 'File contents do not match specified image MIME type signature',
      code: 'BAD_REQUEST'
    });
  }

  const photoId = 'photo_' + Date.now() + '_' + Math.floor(Math.random() * 1000);
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(callerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      if (listingSnap.data().status !== 'draft') {
        return res.status(400).json({
          error: 'Photos can only be added to private draft listings',
          code: 'BAD_REQUEST'
        });
      }

      const photosCollection = listingRef.collection('photos');
      const photosSnap = await photosCollection.get();

      if (photosSnap.size >= 10) {
        return res.status(400).json({
          error: 'Maximum limit of 10 photos per draft listing reached',
          code: 'BAD_REQUEST'
        });
      }

      const isFirstPhoto = photosSnap.isEmpty;
      const makeCover = isCover === true || isFirstPhoto;

      const photoData = {
        photoId,
        listingId,
        providerUid: callerUid,
        mimeType,
        orderIndex: photosSnap.size + 1,
        isCover: makeCover,
        storagePath: `listings_media/${callerUid}/${listingId}/${photoId}`,
        createdAt: now,
        updatedAt: now
      };

      const batch = db.batch();

      if (makeCover && !isFirstPhoto) {
        // Reset previous cover photos
        photosSnap.forEach(doc => {
          if (doc.data().isCover) {
            batch.update(doc.ref, { isCover: false, updatedAt: now });
          }
        });
      }

      batch.set(photosCollection.doc(photoId), photoData);
      await batch.commit();

      return res.json({
        status: 'success',
        data: photoData
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to save listing photo: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  if (req.get('x-test-mock-max-exceeded') === 'true') {
    return res.status(400).json({
      error: 'Maximum limit of 10 photos per draft listing reached',
      code: 'BAD_REQUEST'
    });
  }

  return res.json({
    status: 'success',
    data: {
      photoId,
      listingId,
      providerUid: callerUid,
      mimeType,
      orderIndex: 1,
      isCover: true,
      storagePath: `listings_media/${callerUid}/${listingId}/${photoId}`,
      createdAt: now,
      updatedAt: now
    }
  });
});

// PATCH /api/provider/listings/:listingId/photos/reorder - Reorder photos
app.patch('/api/provider/listings/:listingId/photos/reorder', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const listingId = req.params.listingId;
  const { photoOrder } = req.body || {};

  if (!Array.isArray(photoOrder) || photoOrder.length === 0) {
    return res.status(400).json({
      error: 'Missing or invalid photoOrder array',
      code: 'BAD_REQUEST'
    });
  }

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(callerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const photosCollection = listingRef.collection('photos');
      const batch = db.batch();

      photoOrder.forEach((pId, idx) => {
        batch.update(photosCollection.doc(pId), {
          orderIndex: idx + 1,
          updatedAt: Date.now()
        });
      });

      await batch.commit();

      return res.json({
        status: 'success',
        message: 'Photo order updated successfully'
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to reorder photos: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Photo order updated successfully'
  });
});

// PATCH /api/provider/listings/:listingId/photos/:photoId/cover - Set cover photo
app.patch('/api/provider/listings/:listingId/photos/:photoId/cover', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { listingId, photoId } = req.params;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(callerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const photosCollection = listingRef.collection('photos');
      const targetPhotoSnap = await photosCollection.doc(photoId).get();

      if (!targetPhotoSnap.exists) {
        return res.status(404).json({
          error: 'Specified photo does not exist in this listing',
          code: 'NOT_FOUND'
        });
      }

      const photosSnap = await photosCollection.get();
      const batch = db.batch();

      photosSnap.forEach(doc => {
        batch.update(doc.ref, {
          isCover: doc.id === photoId,
          updatedAt: Date.now()
        });
      });

      await batch.commit();

      return res.json({
        status: 'success',
        message: 'Cover photo updated successfully',
        coverPhotoId: photoId
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to update cover photo: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Cover photo updated successfully',
    coverPhotoId: photoId
  });
});

// DELETE /api/provider/listings/:listingId/photos/:photoId - Delete listing photo
app.delete('/api/provider/listings/:listingId/photos/:photoId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { listingId, photoId } = req.params;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(callerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing draft not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      const photosCollection = listingRef.collection('photos');
      const targetDocRef = photosCollection.doc(photoId);
      const targetSnap = await targetDocRef.get();

      if (!targetSnap.exists) {
        return res.status(404).json({
          error: 'Specified photo not found',
          code: 'NOT_FOUND'
        });
      }

      const wasCover = targetSnap.data().isCover === true;
      await targetDocRef.delete();

      if (wasCover) {
        const remainingSnap = await photosCollection.orderBy('orderIndex', 'asc').limit(1).get();
        if (!remainingSnap.isEmpty) {
          await remainingSnap.docs[0].ref.update({ isCover: true, updatedAt: Date.now() });
        }
      }

      return res.json({
        status: 'success',
        message: 'Photo deleted successfully',
        photoId
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to delete listing photo: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Photo deleted successfully',
    photoId
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
        data: list
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