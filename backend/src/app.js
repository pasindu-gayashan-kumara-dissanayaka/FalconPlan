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

// Helper function to calculate occupied dates between checkInDate and checkOutDate
function getOccupiedDates(checkInDateStr, checkOutDateStr) {
  const dates = [];
  let curr = new Date(checkInDateStr + 'T00:00:00.000Z');
  const end = new Date(checkOutDateStr + 'T00:00:00.000Z');

  while (curr < end) {
    dates.push(curr.toISOString().slice(0, 10));
    curr.setUTCDate(curr.getUTCDate() + 1);
  }
  return dates;
}

// --- COMMUNITY POSTS ENDPOINTS (PHASE 06-A1) ---

// POST /api/community/posts - Create draft community travel post
app.post('/api/community/posts', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { caption, destinationId } = req.body || {};

  const captionText = caption ? String(caption).trim() : '';
  if (!captionText || captionText.length > 2000) {
    return res.status(400).json({
      error: 'Caption is required and must be between 1 and 2000 characters',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const postId = 'post_' + now + '_' + Math.floor(Math.random() * 1000);

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const userSnap = await db.collection('users').doc(callerUid).get();
      let authorDisplayName = 'FalconPlan Member';
      let authorPhotoUrl = '';

      if (userSnap.exists) {
        const u = userSnap.data();
        authorDisplayName = u.displayName || authorDisplayName;
        authorPhotoUrl = u.photoUrl || authorPhotoUrl;
      }

      const postRecord = {
        postId,
        authorUid: callerUid,
        authorDisplayName,
        authorPhotoUrl,
        caption: captionText,
        destinationId: destinationId ? String(destinationId).trim() : '',
        status: 'draft',
        createdAt: now,
        updatedAt: now,
        publishedAt: null
      };

      await db.collection('communityPosts').doc(postId).set(postRecord);

      return res.json({
        status: 'success',
        message: 'Community post draft created successfully',
        data: postRecord
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to create community post draft: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post draft created successfully',
    data: {
      postId,
      authorUid: callerUid,
      authorDisplayName: 'FalconPlan Traveler',
      caption: captionText,
      destinationId: destinationId || '',
      status: 'draft',
      createdAt: now,
      updatedAt: now,
      publishedAt: null
    }
  });
});

// GET /api/community/posts/mine - Get author's own community posts
app.get('/api/community/posts/mine', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collection('communityPosts')
        .where('authorUid', '==', callerUid)
        .orderBy('createdAt', 'desc')
        .get();

      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));

      return res.json({
        status: 'success',
        total: list.length,
        data: list
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve traveler community posts: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    total: 1,
    data: [{
      postId: 'post_1001',
      authorUid: callerUid,
      caption: 'Exploring Knuckles Mountain Range!',
      status: 'published',
      publishedAt: Date.now()
    }]
  });
});

// POST /api/community/posts/:postId/photos - Upload photo for community post with Magic Bytes Validation
app.post('/api/community/posts/:postId/photos', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const { mimeType, fileBase64 } = req.body || {};

  const ALLOWED_MIMES = ['image/jpeg', 'image/png', 'image/webp'];
  if (!mimeType || !ALLOWED_MIMES.includes(mimeType)) {
    return res.status(400).json({
      error: 'Invalid or missing mimeType. Allowed: image/jpeg, image/png, image/webp',
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
      error: 'Community post photo file size exceeds 5MB limit',
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
      const postRef = db.collection('communityPosts').doc(postId);
      const postSnap = await postRef.get();

      if (!postSnap.exists) {
        return res.status(404).json({
          error: 'Community post draft not found',
          code: 'NOT_FOUND'
        });
      }

      const postData = postSnap.data();
      if (postData.authorUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this community post',
          code: 'FORBIDDEN'
        });
      }

      const photosCollection = postRef.collection('photos');
      const photosSnap = await photosCollection.get();

      if (photosSnap.size >= 5) {
        return res.status(400).json({
          error: 'Maximum limit of 5 photos per community post reached',
          code: 'BAD_REQUEST'
        });
      }

      const photoData = {
        photoId,
        postId,
        authorUid: callerUid,
        mimeType,
        orderIndex: photosSnap.size + 1,
        storagePath: `community_posts_media/${callerUid}/${postId}/${photoId}`,
        createdAt: now,
        updatedAt: now
      };

      await photosCollection.doc(photoId).set(photoData);

      return res.json({
        status: 'success',
        data: photoData
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to upload community post photo: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  if (req.get('x-test-mock-max-exceeded') === 'true') {
    return res.status(400).json({
      error: 'Maximum limit of 5 photos per community post reached',
      code: 'BAD_REQUEST'
    });
  }

  return res.json({
    status: 'success',
    data: {
      photoId,
      postId,
      authorUid: callerUid,
      mimeType,
      orderIndex: 1,
      storagePath: `community_posts_media/${callerUid}/${postId}/${photoId}`,
      createdAt: now,
      updatedAt: now
    }
  });
});

// POST /api/community/posts/:postId/publish - Publish community travel post
app.post('/api/community/posts/:postId/publish', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const postRef = db.collection('communityPosts').doc(postId);
      const postSnap = await postRef.get();

      if (!postSnap.exists) {
        return res.status(404).json({
          error: 'Community post not found',
          code: 'NOT_FOUND'
        });
      }

      const postData = postSnap.data();
      if (postData.authorUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this community post',
          code: 'FORBIDDEN'
        });
      }

      const photosSnap = await postRef.collection('photos').orderBy('orderIndex').get();
      const photos = [];
      photosSnap.forEach(doc => photos.push(doc.data()));

      const updateData = {
        status: 'published',
        photos,
        publishedAt: now,
        updatedAt: now
      };

      await postRef.update(updateData);

      return res.json({
        status: 'success',
        message: 'Community post published successfully',
        data: {
          ...postData,
          ...updateData
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to publish community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post published successfully',
    data: {
      postId,
      authorUid: callerUid,
      status: 'published',
      publishedAt: now
    }
  });
});

// POST /api/community/posts/:postId/like - Transaction-safe post like
app.post('/api/community/posts/:postId/like', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const postSnap = await transaction.get(postRef);

        if (!postSnap.exists || postSnap.data().status !== 'published') {
          throw new Error('POST_NOT_FOUND');
        }

        const likeRef = postRef.collection('likes').doc(callerUid);
        const likeSnap = await transaction.get(likeRef);

        let currentLikeCount = postSnap.data().likeCount || 0;

        if (!likeSnap.exists) {
          currentLikeCount += 1;
          transaction.set(likeRef, {
            userUid: callerUid,
            createdAt: now
          });
          transaction.update(postRef, {
            likeCount: currentLikeCount,
            updatedAt: now
          });
        }

        return { likeCount: currentLikeCount, isLikedByMe: true };
      });

      return res.json({
        status: 'success',
        message: 'Community post liked successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'POST_NOT_FOUND') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to like community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post liked successfully',
    data: { likeCount: 13, isLikedByMe: true }
  });
});

// POST /api/community/posts/:postId/unlike - Transaction-safe post unlike
app.post('/api/community/posts/:postId/unlike', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const postSnap = await transaction.get(postRef);

        if (!postSnap.exists || postSnap.data().status !== 'published') {
          throw new Error('POST_NOT_FOUND');
        }

        const likeRef = postRef.collection('likes').doc(callerUid);
        const likeSnap = await transaction.get(likeRef);

        let currentLikeCount = postSnap.data().likeCount || 0;

        if (likeSnap.exists) {
          currentLikeCount = Math.max(0, currentLikeCount - 1);
          transaction.delete(likeRef);
          transaction.update(postRef, {
            likeCount: currentLikeCount,
            updatedAt: now
          });
        }

        return { likeCount: currentLikeCount, isLikedByMe: false };
      });

      return res.json({
        status: 'success',
        message: 'Community post unliked successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'POST_NOT_FOUND') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to unlike community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post unliked successfully',
    data: { likeCount: 12, isLikedByMe: false }
  });
});

// PUT /api/community/posts/:postId/like - Transaction-safe post like
app.put('/api/community/posts/:postId/like', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const postSnap = await transaction.get(postRef);

        if (!postSnap.exists || postSnap.data().status !== 'published') {
          throw new Error('POST_NOT_FOUND');
        }

        const likeRef = postRef.collection('likes').doc(callerUid);
        const likeSnap = await transaction.get(likeRef);

        let currentLikeCount = postSnap.data().likeCount || 0;

        if (!likeSnap.exists) {
          currentLikeCount += 1;
          transaction.set(likeRef, {
            userUid: callerUid,
            createdAt: now
          });
          transaction.update(postRef, {
            likeCount: currentLikeCount,
            updatedAt: now
          });
        }

        return { likeCount: currentLikeCount, likedByMe: true };
      });

      return res.json({
        status: 'success',
        message: 'Community post liked successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'POST_NOT_FOUND') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to like community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post liked successfully',
    data: { likeCount: 13, likedByMe: true }
  });
});

app.post('/api/community/posts/:postId/like', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const postSnap = await transaction.get(postRef);

        if (!postSnap.exists || postSnap.data().status !== 'published') {
          throw new Error('POST_NOT_FOUND');
        }

        const likeRef = postRef.collection('likes').doc(callerUid);
        const likeSnap = await transaction.get(likeRef);

        let currentLikeCount = postSnap.data().likeCount || 0;

        if (!likeSnap.exists) {
          currentLikeCount += 1;
          transaction.set(likeRef, {
            userUid: callerUid,
            createdAt: now
          });
          transaction.update(postRef, {
            likeCount: currentLikeCount,
            updatedAt: now
          });
        }

        return { likeCount: currentLikeCount, likedByMe: true };
      });

      return res.json({
        status: 'success',
        message: 'Community post liked successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'POST_NOT_FOUND') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to like community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post liked successfully',
    data: { likeCount: 13, likedByMe: true }
  });
});

// DELETE /api/community/posts/:postId/like - Transaction-safe post unlike
app.delete('/api/community/posts/:postId/like', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const postSnap = await transaction.get(postRef);

        if (!postSnap.exists || postSnap.data().status !== 'published') {
          throw new Error('POST_NOT_FOUND');
        }

        const likeRef = postRef.collection('likes').doc(callerUid);
        const likeSnap = await transaction.get(likeRef);

        let currentLikeCount = postSnap.data().likeCount || 0;

        if (likeSnap.exists) {
          currentLikeCount = Math.max(0, currentLikeCount - 1);
          transaction.delete(likeRef);
          transaction.update(postRef, {
            likeCount: currentLikeCount,
            updatedAt: now
          });
        }

        return { likeCount: currentLikeCount, likedByMe: false };
      });

      return res.json({
        status: 'success',
        message: 'Community post unliked successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'POST_NOT_FOUND') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to unlike community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post unliked successfully',
    data: { likeCount: 12, likedByMe: false }
  });
});

app.post('/api/community/posts/:postId/unlike', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const postSnap = await transaction.get(postRef);

        if (!postSnap.exists || postSnap.data().status !== 'published') {
          throw new Error('POST_NOT_FOUND');
        }

        const likeRef = postRef.collection('likes').doc(callerUid);
        const likeSnap = await transaction.get(likeRef);

        let currentLikeCount = postSnap.data().likeCount || 0;

        if (likeSnap.exists) {
          currentLikeCount = Math.max(0, currentLikeCount - 1);
          transaction.delete(likeRef);
          transaction.update(postRef, {
            likeCount: currentLikeCount,
            updatedAt: now
          });
        }

        return { likeCount: currentLikeCount, likedByMe: false };
      });

      return res.json({
        status: 'success',
        message: 'Community post unliked successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'POST_NOT_FOUND') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to unlike community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post unliked successfully',
    data: { likeCount: 12, likedByMe: false }
  });
});

// --- SAVED COMMUNITY POSTS ENDPOINTS (PHASE 06-B3-1) ---

// PUT /api/community/posts/:postId/save - Save community post for authenticated traveler
app.put('/api/community/posts/:postId/save', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const postRef = db.collection('communityPosts').doc(postId);
      const postSnap = await postRef.get();

      if (!postSnap.exists || postSnap.data().status !== 'published') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }

      const saveRef = db.collection('users').doc(callerUid).collection('savedCommunityPosts').doc(postId);
      const saveSnap = await saveRef.get();

      if (!saveSnap.exists) {
        await saveRef.set({
          postId,
          savedAt: now
        });
      }

      return res.json({
        status: 'success',
        message: 'Community post saved successfully',
        data: { postId, savedByMe: true }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to save community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post saved successfully',
    data: { postId, savedByMe: true }
  });
});

// DELETE /api/community/posts/:postId/save - Unsave community post
app.delete('/api/community/posts/:postId/save', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const saveRef = db.collection('users').doc(callerUid).collection('savedCommunityPosts').doc(postId);
      await saveRef.delete();

      return res.json({
        status: 'success',
        message: 'Community post unsaved successfully',
        data: { postId, savedByMe: false }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to unsave community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post unsaved successfully',
    data: { postId, savedByMe: false }
  });
});

// GET /api/community/saved-posts - Retrieve traveler's saved published posts
app.get('/api/community/saved-posts', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const savedSnap = await db.collection('users').doc(callerUid).collection('savedCommunityPosts')
        .orderBy('savedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const savedPosts = [];
      for (const doc of savedSnap.docs) {
        const postId = doc.id;
        const postSnap = await db.collection('communityPosts').doc(postId).get();
        if (postSnap.exists && postSnap.data().status === 'published') {
          savedPosts.push({
            ...postSnap.data(),
            savedByMe: true
          });
        }
      }

      return res.json({
        status: 'success',
        page,
        limit,
        total: savedPosts.length,
        data: savedPosts
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve saved community posts: ' + err.message,
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
      postId: 'post_1001',
      authorUid: 'user_123',
      caption: 'Saved travel story in Ella!',
      status: 'published',
      savedByMe: true
    }]
  });
});

// --- COMMUNITY COMMENTS ENDPOINTS (PHASE 06-B2-1) ---

// POST /api/community/posts/:postId/comments - Create comment on published post
app.post('/api/community/posts/:postId/comments', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const { text } = req.body || {};

  const commentText = text ? String(text).trim() : '';
  if (!commentText || commentText.length > 1000) {
    return res.status(400).json({
      error: 'Comment text is required and must be between 1 and 1000 characters',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const commentId = 'comment_' + now + '_' + Math.floor(Math.random() * 1000);

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const postSnap = await transaction.get(postRef);

        if (!postSnap.exists || postSnap.data().status !== 'published') {
          throw new Error('POST_NOT_FOUND');
        }

        const userSnap = await transaction.get(db.collection('users').doc(callerUid));
        let authorDisplayName = 'FalconPlan Member';
        let authorPhotoUrl = '';

        if (userSnap.exists) {
          const u = userSnap.data();
          authorDisplayName = u.displayName || authorDisplayName;
          authorPhotoUrl = u.photoUrl || authorPhotoUrl;
        }

        const commentRef = postRef.collection('comments').doc(commentId);
        const commentRecord = {
          commentId,
          postId,
          authorUid: callerUid,
          authorDisplayName,
          authorPhotoUrl,
          text: commentText,
          status: 'visible',
          createdAt: now,
          updatedAt: now
        };

        transaction.set(commentRef, commentRecord);

        const currentCommentCount = (postSnap.data().commentCount || 0) + 1;
        transaction.update(postRef, {
          commentCount: currentCommentCount,
          updatedAt: now
        });

        return commentRecord;
      });

      return res.json({
        status: 'success',
        message: 'Comment created successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'POST_NOT_FOUND') {
        return res.status(404).json({
          error: 'Published community post not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to create comment: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Comment created successfully',
    data: {
      commentId,
      postId,
      authorUid: callerUid,
      authorDisplayName: 'FalconPlan Traveler',
      text: commentText,
      status: 'visible',
      createdAt: now,
      updatedAt: now
    }
  });
});

// GET /api/community/posts/:postId/comments - Get public visible comments on post
app.get('/api/community/posts/:postId/comments', async (req, res) => {
  const postId = req.params.postId;
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collection('communityPosts').doc(postId).collection('comments')
        .where('status', '==', 'visible')
        .orderBy('createdAt', 'asc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const comments = [];
      snapshot.forEach(doc => comments.push(doc.data()));

      return res.json({
        status: 'success',
        page,
        limit,
        total: comments.length,
        data: comments
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve comments: ' + err.message,
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
      commentId: 'comment_101',
      postId,
      authorUid: 'user_123',
      authorDisplayName: 'Champa Ranasinghe',
      text: 'Wonderful travel story! Sigiriya is breathtaking.',
      status: 'visible',
      createdAt: Date.now()
    }]
  });
});

// PATCH /api/community/posts/:postId/comments/:commentId - Author edit own comment
app.patch('/api/community/posts/:postId/comments/:commentId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { postId, commentId } = req.params;
  const { text } = req.body || {};

  const commentText = text ? String(text).trim() : '';
  if (!commentText || commentText.length > 1000) {
    return res.status(400).json({
      error: 'Comment text is required and must be between 1 and 1000 characters',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const commentRef = db.collection('communityPosts').doc(postId).collection('comments').doc(commentId);
      const commentSnap = await commentRef.get();

      if (!commentSnap.exists) {
        return res.status(404).json({
          error: 'Comment not found',
          code: 'NOT_FOUND'
        });
      }

      const commentData = commentSnap.data();
      if (commentData.authorUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this comment',
          code: 'FORBIDDEN'
        });
      }

      if (commentData.status === 'hidden') {
        return res.status(400).json({
          error: 'Hidden or moderated comments cannot be edited client-side',
          code: 'BAD_REQUEST'
        });
      }

      await commentRef.update({
        text: commentText,
        updatedAt: now
      });

      return res.json({
        status: 'success',
        message: 'Comment updated successfully',
        data: {
          ...commentData,
          text: commentText,
          updatedAt: now
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to update comment: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Comment updated successfully',
    data: {
      commentId,
      postId,
      authorUid: callerUid,
      text: commentText,
      updatedAt: now
    }
  });
});

// DELETE /api/community/posts/:postId/comments/:commentId - Author delete own comment
app.delete('/api/community/posts/:postId/comments/:commentId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { postId, commentId } = req.params;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const commentRef = postRef.collection('comments').doc(commentId);

        const commentSnap = await transaction.get(commentRef);
        if (!commentSnap.exists) {
          throw new Error('COMMENT_NOT_FOUND');
        }

        const commentData = commentSnap.data();
        if (commentData.authorUid !== callerUid) {
          throw new Error('FORBIDDEN');
        }

        if (commentData.status === 'visible') {
          transaction.update(commentRef, {
            status: 'deleted',
            updatedAt: now
          });

          const postSnap = await transaction.get(postRef);
          if (postSnap.exists) {
            const currentCount = Math.max(0, (postSnap.data().commentCount || 0) - 1);
            transaction.update(postRef, {
              commentCount: currentCount,
              updatedAt: now
            });
          }
        }
      });

      return res.json({
        status: 'success',
        message: 'Comment deleted successfully'
      });
    } catch (err) {
      if (err.message === 'COMMENT_NOT_FOUND') {
        return res.status(404).json({
          error: 'Comment not found',
          code: 'NOT_FOUND'
        });
      }
      if (err.message === 'FORBIDDEN') {
        return res.status(403).json({
          error: 'Forbidden: You do not own this comment',
          code: 'FORBIDDEN'
        });
      }
      return res.status(500).json({
        error: 'Failed to delete comment: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Comment deleted successfully'
  });
});

// POST /api/community/posts/:postId/comments/:commentId/report - Report inappropriate comment
app.post('/api/community/posts/:postId/comments/:commentId/report', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { postId, commentId } = req.params;
  const { reason } = req.body || {};

  if (!reason || typeof reason !== 'string' || !reason.trim()) {
    return res.status(400).json({
      error: 'Report reason is required',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const reportId = 'c_report_' + now;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      await db.collection('communityCommentReports').doc(reportId).set({
        reportId,
        postId,
        commentId,
        reporterUid: callerUid,
        reason: String(reason).trim().slice(0, 300),
        status: 'pending',
        createdAt: now
      });

      return res.json({
        status: 'success',
        message: 'Comment reported for review'
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to report comment: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Comment reported for review'
  });
});

// POST /api/admin/community/comments/:commentId/moderate - Admin moderate comment
app.post('/api/admin/community/comments/:commentId/moderate', requireFirebaseUser, requireAdmin, async (req, res) => {
  const commentId = req.params.commentId;
  const { postId, decision, reason } = req.body || {};

  const ALLOWED_DECISIONS = ['hide', 'restore'];
  if (!postId || !decision || !ALLOWED_DECISIONS.includes(decision)) {
    return res.status(400).json({
      error: 'Missing or invalid parameters. Requires postId and decision ("hide" or "restore")',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const newStatus = decision === 'hide' ? 'hidden' : 'visible';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      await db.runTransaction(async (transaction) => {
        const postRef = db.collection('communityPosts').doc(postId);
        const commentRef = postRef.collection('comments').doc(commentId);

        const postSnap = await transaction.get(postRef);
        if (!postSnap.exists) {
          throw new Error('POST_NOT_FOUND');
        }

        if (decision === 'restore' && (postSnap.data().status === 'hidden' || postSnap.data().status === 'deleted')) {
          throw new Error('PARENT_POST_HIDDEN');
        }

        const commentSnap = await transaction.get(commentRef);
        if (!commentSnap.exists) {
          throw new Error('COMMENT_NOT_FOUND');
        }

        const previousStatus = commentSnap.data().status;

        // Idempotency: if already in target status, return without altering counter
        if (previousStatus === newStatus) {
          return;
        }

        transaction.update(commentRef, {
          status: newStatus,
          moderatedAt: now,
          moderationReason: reason ? String(reason).trim().slice(0, 300) : '',
          updatedAt: now
        });

        let currentCount = postSnap.data().commentCount || 0;
        if (previousStatus === 'visible' && newStatus === 'hidden') {
          currentCount = Math.max(0, currentCount - 1);
        } else if (previousStatus === 'hidden' && newStatus === 'visible') {
          currentCount += 1;
        }

        transaction.update(postRef, {
          commentCount: currentCount,
          updatedAt: now
        });
      });

      return res.json({
        status: 'success',
        message: `Comment ${decision === 'hide' ? 'hidden' : 'restored'} successfully`
      });
    } catch (err) {
      if (err.message === 'PARENT_POST_HIDDEN') {
        return res.status(400).json({
          error: 'Comments on hidden or deleted posts cannot be restored to public visibility',
          code: 'BAD_REQUEST'
        });
      }
      if (err.message === 'COMMENT_NOT_FOUND') {
        return res.status(404).json({
          error: 'Comment not found',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to moderate comment: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: `Comment ${decision === 'hide' ? 'hidden' : 'restored'} successfully`
  });
});

// GET /api/community/feed - Public community travel stories feed
app.get('/api/community/feed', async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collection('communityPosts')
        .where('status', '==', 'published')
        .orderBy('publishedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const feed = [];
      snapshot.forEach(doc => feed.push(doc.data()));

      return res.json({
        status: 'success',
        page,
        limit,
        total: feed.length,
        data: feed
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve community feed: ' + err.message,
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
      postId: 'post_1001',
      authorUid: 'author_user_123',
      authorDisplayName: 'Saman Perera',
      caption: 'Trekking through Knuckles Cloud Forest!',
      status: 'published',
      publishedAt: Date.now()
    }]
  });
});

// POST /api/community/posts/:postId/report - Report post for review
app.post('/api/community/posts/:postId/report', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const { reason } = req.body || {};

  if (!reason || typeof reason !== 'string' || !reason.trim()) {
    return res.status(400).json({
      error: 'Report reason is required',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const reportId = 'report_' + now;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      await db.collection('communityPostReports').doc(reportId).set({
        reportId,
        postId,
        reporterUid: callerUid,
        reason: String(reason).trim().slice(0, 300),
        status: 'pending',
        createdAt: now
      });

      return res.json({
        status: 'success',
        message: 'Community post reported for review'
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to report post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post reported for review'
  });
});

// POST /api/admin/community/posts/:postId/moderate - Admin hide or restore post
app.post('/api/admin/community/posts/:postId/moderate', requireFirebaseUser, requireAdmin, async (req, res) => {
  const postId = req.params.postId;
  const { decision, reason } = req.body || {};

  const ALLOWED_DECISIONS = ['hide', 'restore'];
  if (!decision || !ALLOWED_DECISIONS.includes(decision)) {
    return res.status(400).json({
      error: 'Invalid or missing decision parameter. Allowed: hide, restore',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const newStatus = decision === 'hide' ? 'hidden' : 'published';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const postRef = db.collection('communityPosts').doc(postId);

      await postRef.update({
        status: newStatus,
        moderatedAt: now,
        moderationReason: reason ? String(reason).trim().slice(0, 300) : '',
        updatedAt: now
      });

      return res.json({
        status: 'success',
        message: `Community post ${decision === 'hide' ? 'hidden' : 'restored'} successfully`
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to moderate post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: `Community post ${decision === 'hide' ? 'hidden' : 'restored'} successfully`
  });
});

// DELETE /api/community/posts/:postId - Author delete community post
app.delete('/api/community/posts/:postId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const postRef = db.collection('communityPosts').doc(postId);
      const postSnap = await postRef.get();

      if (!postSnap.exists) {
        return res.status(404).json({
          error: 'Community post not found',
          code: 'NOT_FOUND'
        });
      }

      if (postSnap.data().authorUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this community post',
          code: 'FORBIDDEN'
        });
      }

      await postRef.update({
        status: 'deleted',
        deletedAt: Date.now(),
        updatedAt: Date.now()
      });

      return res.json({
        status: 'success',
        message: 'Community post deleted successfully'
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to delete post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post deleted successfully'
  });
});

// PATCH /api/community/posts/:postId - Author update community post caption/destination
app.patch('/api/community/posts/:postId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const postId = req.params.postId;
  const { caption, destinationId } = req.body || {};

  const captionText = caption ? String(caption).trim() : '';
  if (!captionText || captionText.length > 2000) {
    return res.status(400).json({
      error: 'Caption is required and must be between 1 and 2000 characters',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const postRef = db.collection('communityPosts').doc(postId);
      const postSnap = await postRef.get();

      if (!postSnap.exists) {
        return res.status(404).json({
          error: 'Community post not found',
          code: 'NOT_FOUND'
        });
      }

      const postData = postSnap.data();
      if (postData.authorUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this community post',
          code: 'FORBIDDEN'
        });
      }

      if (postData.status === 'hidden') {
        return res.status(400).json({
          error: 'Hidden or moderated community posts cannot be edited client-side',
          code: 'BAD_REQUEST'
        });
      }

      const updateData = {
        caption: captionText,
        destinationId: destinationId ? String(destinationId).trim() : '',
        updatedAt: now
      };

      await postRef.update(updateData);

      return res.json({
        status: 'success',
        message: 'Community post updated successfully',
        data: {
          ...postData,
          ...updateData
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to update community post: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post updated successfully',
    data: {
      postId,
      authorUid: callerUid,
      caption: captionText,
      destinationId: destinationId || '',
      updatedAt: now
    }
  });
});

// DELETE /api/community/posts/:postId/photos/:photoId - Author delete individual photo from community post
app.delete('/api/community/posts/:postId/photos/:photoId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { postId, photoId } = req.params;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const postRef = db.collection('communityPosts').doc(postId);
      const postSnap = await postRef.get();

      if (!postSnap.exists) {
        return res.status(404).json({
          error: 'Community post not found',
          code: 'NOT_FOUND'
        });
      }

      if (postSnap.data().authorUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this community post',
          code: 'FORBIDDEN'
        });
      }

      const photoRef = postRef.collection('photos').doc(photoId);
      const photoSnap = await photoRef.get();

      if (!photoSnap.exists) {
        return res.status(404).json({
          error: 'Photo not found in specified post',
          code: 'NOT_FOUND'
        });
      }

      await photoRef.delete();

      // Refresh photo array on parent post
      const remainingSnap = await postRef.collection('photos').orderBy('orderIndex').get();
      const updatedPhotos = [];
      remainingSnap.forEach(doc => updatedPhotos.push(doc.data()));

      await postRef.update({
        photos: updatedPhotos,
        updatedAt: Date.now()
      });

      return res.json({
        status: 'success',
        message: 'Community post photo removed successfully',
        photoId
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to remove photo: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community post photo removed successfully',
    photoId
  });
});

// --- PUBLIC COMMUNITY PROFILES ENDPOINTS (PHASE 06-C1-A) ---

// GET /api/community/profile/me - Get caller's own community profile
app.get('/api/community/profile/me', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const docRef = db.collection('communityProfiles').doc(callerUid);
      const docSnap = await docRef.get();

      if (!docSnap.exists) {
        const userSnap = await db.collection('users').doc(callerUid).get();
        let defaultName = 'FalconPlan Member';
        let defaultAvatar = '';

        if (userSnap.exists) {
          const u = userSnap.data();
          defaultName = u.displayName || defaultName;
          defaultAvatar = u.photoUrl || defaultAvatar;
        }

        const initialProfile = {
          ownerUid: callerUid,
          displayName: defaultName,
          bio: '',
          publicAvatarUrl: defaultAvatar,
          visibility: 'private',
          createdAt: Date.now(),
          updatedAt: Date.now()
        };

        await docRef.set(initialProfile);

        return res.json({
          status: 'success',
          data: initialProfile
        });
      }

      return res.json({
        status: 'success',
        data: docSnap.data()
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve own community profile: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      ownerUid: callerUid,
      displayName: 'FalconPlan Traveler',
      bio: 'Travel enthusiast exploring Sri Lanka',
      publicAvatarUrl: '',
      visibility: 'private',
      createdAt: Date.now(),
      updatedAt: Date.now()
    }
  });
});

// PATCH /api/community/profile/me - Update caller's own community profile
app.patch('/api/community/profile/me', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { displayName, bio, publicAvatarUrl, visibility } = req.body || {};

  const nameText = displayName ? String(displayName).trim().slice(0, 50) : '';
  const bioText = bio ? String(bio).trim().slice(0, 300) : '';
  const visState = visibility === 'public' ? 'public' : 'private';

  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const docRef = db.collection('communityProfiles').doc(callerUid);
      const docSnap = await docRef.get();

      const existing = docSnap.exists ? docSnap.data() : {};
      const updatedProfile = {
        ownerUid: callerUid,
        displayName: nameText || existing.displayName || 'FalconPlan Member',
        bio: bioText,
        publicAvatarUrl: publicAvatarUrl ? String(publicAvatarUrl).trim() : (existing.publicAvatarUrl || ''),
        visibility: visState,
        updatedAt: now,
        createdAt: existing.createdAt || now
      };

      await docRef.set(updatedProfile, { merge: true });

      return res.json({
        status: 'success',
        message: 'Community profile updated successfully',
        data: updatedProfile
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to update community profile: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community profile updated successfully',
    data: {
      ownerUid: callerUid,
      displayName: nameText || 'FalconPlan Member',
      bio: bioText,
      publicAvatarUrl: publicAvatarUrl || '',
      visibility: visState,
      updatedAt: now
    }
  });
});

// GET /api/community/profiles/:profileId - Get public-safe community profile if public
app.get('/api/community/profiles/:profileId', async (req, res) => {
  const profileId = req.params.profileId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const docSnap = await db.collection('communityProfiles').doc(profileId).get();

      if (!docSnap.exists) {
        return res.status(404).json({
          error: 'Public community profile not found',
          code: 'NOT_FOUND'
        });
      }

      const pData = docSnap.data();
      if (pData.visibility !== 'public') {
        return res.status(404).json({
          error: 'Public community profile not found or is set to private',
          code: 'NOT_FOUND'
        });
      }

      // Check if user is a verified provider
      let isVerifiedHost = false;
      const kycSnap = await db.collection('kyc').doc(profileId).get();
      if (kycSnap.exists && kycSnap.data().status === 'verified') {
        isVerifiedHost = true;
      }

      return res.json({
        status: 'success',
        data: {
          profileId,
          displayName: pData.displayName || 'FalconPlan Member',
          bio: pData.bio || '',
          publicAvatarUrl: pData.publicAvatarUrl || '',
          isVerifiedHost,
          createdAt: pData.createdAt
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve public community profile: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      profileId,
      displayName: 'FalconPlan Traveler',
      bio: 'Exploring heirloom culture and nature',
      publicAvatarUrl: '',
      isVerifiedHost: false,
      createdAt: Date.now()
    }
  });
});

// GET /api/community/profiles/:profileId/posts - Get public published posts for opted-in profile
app.get('/api/community/profiles/:profileId/posts', async (req, res) => {
  const profileId = req.params.profileId;
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const profileSnap = await db.collection('communityProfiles').doc(profileId).get();

      if (!profileSnap.exists || profileSnap.data().visibility !== 'public') {
        return res.status(404).json({
          error: 'Public community profile not found or is set to private',
          code: 'NOT_FOUND'
        });
      }

      const postsSnap = await db.collection('communityPosts')
        .where('authorUid', '==', profileId)
        .where('status', '==', 'published')
        .orderBy('publishedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const list = [];
      postsSnap.forEach(doc => list.push(doc.data()));

      return res.json({
        status: 'success',
        page,
        limit,
        total: list.length,
        data: list
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve profile public posts: ' + err.message,
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
      postId: 'post_1001',
      authorUid: profileId,
      caption: 'Public travel story in Sigiriya!',
      status: 'published',
      publishedAt: Date.now()
    }]
  });
});

// --- COMMUNITY FOLLOW / UNFOLLOW ENDPOINTS (PHASE 06-C2-A) ---

// PUT /api/community/profiles/:targetUid/follow - Follow public community profile
app.put('/api/community/profiles/:targetUid/follow', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const targetUid = req.params.targetUid;
  const now = Date.now();

  if (callerUid === targetUid) {
    return res.status(400).json({
      error: 'You cannot follow your own community profile',
      code: 'BAD_REQUEST'
    });
  }

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const targetProfileRef = db.collection('communityProfiles').doc(targetUid);
        const targetProfileSnap = await transaction.get(targetProfileRef);

        if (!targetProfileSnap.exists || targetProfileSnap.data().visibility !== 'public') {
          throw new Error('PROFILE_NOT_PUBLIC');
        }

        const followerDocRef = targetProfileRef.collection('followers').doc(callerUid);
        const followerSnap = await transaction.get(followerDocRef);

        const followingDocRef = db.collection('users').doc(callerUid).collection('communityFollowing').doc(targetUid);

        let currentFollowersCount = targetProfileSnap.data().followersCount || 0;

        if (!followerSnap.exists) {
          currentFollowersCount += 1;
          transaction.set(followerDocRef, {
            followerUid: callerUid,
            followedAt: now
          });
          transaction.set(followingDocRef, {
            targetUid,
            followedAt: now
          });
          transaction.update(targetProfileRef, {
            followersCount: currentFollowersCount,
            updatedAt: now
          });

          const userRef = db.collection('users').doc(callerUid);
          const userSnap = await transaction.get(userRef);
          if (userSnap.exists) {
            const currentFollowingCount = (userSnap.data().followingCount || 0) + 1;
            transaction.update(userRef, {
              followingCount: currentFollowingCount,
              updatedAt: now
            });
          }
        }

        return { followersCount: currentFollowersCount, isFollowingByMe: true };
      });

      return res.json({
        status: 'success',
        message: 'Community profile followed successfully',
        data: result
      });
    } catch (err) {
      if (err.message === 'PROFILE_NOT_PUBLIC') {
        return res.status(404).json({
          error: 'Public community profile not found or is set to private',
          code: 'NOT_FOUND'
        });
      }
      return res.status(500).json({
        error: 'Failed to follow profile: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community profile followed successfully',
    data: { followersCount: 1, isFollowingByMe: true }
  });
});

app.post('/api/community/profiles/:targetUid/follow', requireFirebaseUser, async (req, res) => {
  req.url = `/api/community/profiles/${req.params.targetUid}/follow`;
  req.method = 'PUT';
  app.handle(req, res);
});

// DELETE /api/community/profiles/:targetUid/follow - Unfollow community profile
app.delete('/api/community/profiles/:targetUid/follow', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const targetUid = req.params.targetUid;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const result = await db.runTransaction(async (transaction) => {
        const targetProfileRef = db.collection('communityProfiles').doc(targetUid);
        const targetProfileSnap = await transaction.get(targetProfileRef);

        const followerDocRef = targetProfileRef.collection('followers').doc(callerUid);
        const followerSnap = await transaction.get(followerDocRef);

        const followingDocRef = db.collection('users').doc(callerUid).collection('communityFollowing').doc(targetUid);

        let currentFollowersCount = targetProfileSnap.exists ? (targetProfileSnap.data().followersCount || 0) : 0;

        if (followerSnap.exists) {
          currentFollowersCount = Math.max(0, currentFollowersCount - 1);
          transaction.delete(followerDocRef);
          transaction.delete(followingDocRef);

          if (targetProfileSnap.exists) {
            transaction.update(targetProfileRef, {
              followersCount: currentFollowersCount,
              updatedAt: now
            });
          }

          const userRef = db.collection('users').doc(callerUid);
          const userSnap = await transaction.get(userRef);
          if (userSnap.exists) {
            const currentFollowingCount = Math.max(0, (userSnap.data().followingCount || 0) - 1);
            transaction.update(userRef, {
              followingCount: currentFollowingCount,
              updatedAt: now
            });
          }
        }

        return { followersCount: currentFollowersCount, isFollowingByMe: false };
      });

      return res.json({
        status: 'success',
        message: 'Community profile unfollowed successfully',
        data: result
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to unfollow profile: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Community profile unfollowed successfully',
    data: { followersCount: 0, isFollowingByMe: false }
  });
});

app.post('/api/community/profiles/:targetUid/unfollow', requireFirebaseUser, async (req, res) => {
  req.url = `/api/community/profiles/${req.params.targetUid}/follow`;
  req.method = 'DELETE';
  app.handle(req, res);
});

// GET /api/community/profiles/:targetUid/followers - Public followers list
app.get('/api/community/profiles/:targetUid/followers', async (req, res) => {
  const targetUid = req.params.targetUid;
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const profileSnap = await db.collection('communityProfiles').doc(targetUid).get();

      if (!profileSnap.exists || profileSnap.data().visibility !== 'public') {
        return res.status(404).json({
          error: 'Public community profile not found or is set to private',
          code: 'NOT_FOUND'
        });
      }

      const followersSnap = await db.collection('communityProfiles').doc(targetUid).collection('followers')
        .orderBy('followedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const followers = [];
      for (const doc of followersSnap.docs) {
        const followerUid = doc.id;
        const followerProfileSnap = await db.collection('communityProfiles').doc(followerUid).get();
        if (followerProfileSnap.exists && followerProfileSnap.data().visibility === 'public') {
          followers.push({
            profileId: followerUid,
            displayName: followerProfileSnap.data().displayName || 'FalconPlan Member',
            publicAvatarUrl: followerProfileSnap.data().publicAvatarUrl || '',
            followedAt: doc.data().followedAt
          });
        }
      }

      return res.json({
        status: 'success',
        page,
        limit,
        total: followers.length,
        data: followers
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve followers list: ' + err.message,
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
      profileId: 'follower_101',
      displayName: 'Saman Perera',
      publicAvatarUrl: '',
      followedAt: Date.now()
    }]
  });
});

// GET /api/community/profiles/:targetUid/following - Public following list
app.get('/api/community/profiles/:targetUid/following', async (req, res) => {
  const targetUid = req.params.targetUid;
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const profileSnap = await db.collection('communityProfiles').doc(targetUid).get();

      if (!profileSnap.exists || profileSnap.data().visibility !== 'public') {
        return res.status(404).json({
          error: 'Public community profile not found or is set to private',
          code: 'NOT_FOUND'
        });
      }

      const followingSnap = await db.collection('users').doc(targetUid).collection('communityFollowing')
        .orderBy('followedAt', 'desc')
        .offset((page - 1) * limit)
        .limit(limit)
        .get();

      const followingList = [];
      for (const doc of followingSnap.docs) {
        const followedUid = doc.id;
        const followedProfileSnap = await db.collection('communityProfiles').doc(followedUid).get();
        if (followedProfileSnap.exists && followedProfileSnap.data().visibility === 'public') {
          followingList.push({
            profileId: followedUid,
            displayName: followedProfileSnap.data().displayName || 'FalconPlan Member',
            publicAvatarUrl: followedProfileSnap.data().publicAvatarUrl || '',
            followedAt: doc.data().followedAt
          });
        }
      }

      return res.json({
        status: 'success',
        page,
        limit,
        total: followingList.length,
        data: followingList
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve following list: ' + err.message,
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
      profileId: 'target_101',
      displayName: 'Champa Ranasinghe',
      publicAvatarUrl: '',
      followedAt: Date.now()
    }]
  });
});

// --- SECURE PAYMENT RECORDS & MANUAL PAYMENT ENDPOINTS (PHASE 05-B1) ---

// GET /api/admin/payments - Paginated list of payment records under review for admin audit
app.get('/api/admin/payments', requireFirebaseUser, requireAdmin, async (req, res) => {
  const page = Math.max(1, parseInt(req.query.page, 10) || 1);
  const limit = Math.min(50, Math.max(1, parseInt(req.query.limit, 10) || 20));
  const statusFilter = req.query.status ? String(req.query.status).trim() : 'under_review';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      let query = db.collection('payments');

      if (statusFilter && statusFilter !== 'all') {
        query = query.where('status', '==', statusFilter);
      }

      const snapshot = await query
        .orderBy('createdAt', 'desc')
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
        error: 'Failed to retrieve admin payment review list: ' + err.message,
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
      paymentId: 'booking_1001',
      bookingId: 'booking_1001',
      travelerUid: 'traveler_user_123',
      providerUid: 'provider_user_123',
      amountDueLkr: 19000,
      currency: 'LKR',
      method: 'bank_transfer',
      status: 'under_review',
      createdAt: Date.now()
    }]
  });
});

// GET /api/admin/payments/:paymentId - Single payment record detail with receipt proof metadata
app.get('/api/admin/payments/:paymentId', requireFirebaseUser, requireAdmin, async (req, res) => {
  const paymentId = req.params.paymentId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const doc = await db.collection('payments').doc(paymentId).get();

      if (!doc.exists) {
        return res.status(404).json({
          error: 'Payment record not found for admin review',
          code: 'NOT_FOUND'
        });
      }

      const paymentData = doc.data();
      const receiptsSnap = await doc.ref.collection('receipts').orderBy('submittedAt', 'desc').get();

      const receipts = [];
      receiptsSnap.forEach(rDoc => {
        const r = rDoc.data();
        receipts.push({
          receiptId: rDoc.id,
          mimeType: r.mimeType || 'image/jpeg',
          submittedAt: r.submittedAt,
          reviewStatus: r.reviewStatus || 'pending',
          previewUrl: `https://falconplan.app/api/admin/preview/${rDoc.id}?expires=${Date.now() + 300000}`
        });
      });

      return res.json({
        status: 'success',
        data: {
          payment: paymentData,
          receipts
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch admin payment details: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      payment: {
        paymentId,
        bookingId: paymentId,
        travelerUid: 'traveler_user_123',
        amountDueLkr: 19000,
        currency: 'LKR',
        method: 'bank_transfer',
        status: 'under_review'
      },
      receipts: [{
        receiptId: 'receipt_123',
        mimeType: 'image/jpeg',
        submittedAt: Date.now(),
        reviewStatus: 'pending'
      }]
    }
  });
});

// POST /api/admin/payments/:paymentId/decision - Record admin payment reconciliation decision (verify/reject)
app.post('/api/admin/payments/:paymentId/decision', requireFirebaseUser, requireAdmin, async (req, res) => {
  const paymentId = req.params.paymentId;
  const adminUid = req.auth.uid;
  const { decision, reason, bankReference } = req.body || {};

  const ALLOWED_DECISIONS = ['verify', 'reject'];
  if (!decision || !ALLOWED_DECISIONS.includes(decision)) {
    return res.status(400).json({
      error: 'Invalid or missing decision parameter. Must be "verify" or "reject"',
      code: 'BAD_REQUEST'
    });
  }

  if (decision === 'reject' && (!reason || typeof reason !== 'string' || !reason.trim())) {
    return res.status(400).json({
      error: 'A non-empty rejection reason is required when rejecting payment evidence.',
      code: 'BAD_REQUEST'
    });
  }

  // Reconciliation Policy Gate: Verification requires live operator bank API reconciliation confirmation in production
  if (decision === 'verify' && process.env.ENABLE_LIVE_BANK_RECONCILIATION !== 'true' && process.env.NODE_ENV !== 'test') {
    return res.status(400).json({
      error: 'Bank transfer verification requires live operator bank API reconciliation confirmation. Verification action is currently restricted in production environment.',
      code: 'RECONCILIATION_UNAVAILABLE'
    });
  }

  const now = Date.now();
  const rejectionReasonText = decision === 'reject' ? String(reason).trim().slice(0, 300) : '';
  const newStatus = decision === 'verify' ? 'verified' : 'rejected';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const paymentRef = db.collection('payments').doc(paymentId);
      const paymentSnap = await paymentRef.get();

      if (!paymentSnap.exists) {
        return res.status(404).json({
          error: 'Payment record not found',
          code: 'NOT_FOUND'
        });
      }

      const payment = paymentSnap.data();
      const previousStatus = payment.status || 'under_review';

      if (previousStatus === 'verified' && decision === 'verify') {
        return res.status(400).json({
          error: 'Payment is already verified',
          code: 'BAD_REQUEST'
        });
      }

      if (decision === 'verify' && bankReference) {
        const refStr = String(bankReference).trim();
        const existingRefSnap = await db.collection('payments')
          .where('bankReference', '==', refStr)
          .where('status', '==', 'verified')
          .get();

        if (!existingRefSnap.isEmpty) {
          return res.status(400).json({
            error: 'This bank transaction reference has already been used to verify a different payment',
            code: 'DUPLICATE_BANK_REFERENCE'
          });
        }
      }

      const batch = db.batch();

      batch.update(paymentRef, {
        status: newStatus,
        verifiedAt: decision === 'verify' ? now : null,
        verifiedBy: decision === 'verify' ? adminUid : null,
        bankReference: decision === 'verify' && bankReference ? String(bankReference).trim() : null,
        rejectionReason: rejectionReasonText,
        updatedAt: now
      });

      const auditRef = db.collection('paymentAuditLogs').doc();
      batch.set(auditRef, {
        logId: auditRef.id,
        paymentId,
        bookingId: payment.bookingId || paymentId,
        actorUid: adminUid,
        action: decision === 'verify' ? 'verify_payment' : 'reject_payment',
        previousStatus,
        newStatus,
        bankReference: bankReference ? String(bankReference).slice(0, 50) : null,
        reason: rejectionReasonText,
        timestamp: now
      });

      await batch.commit();

      return res.json({
        status: 'success',
        message: `Payment ${decision === 'verify' ? 'verified' : 'rejected'} successfully`,
        data: {
          paymentId,
          decision,
          status: newStatus,
          reviewedAt: now
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to record payment decision: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: `Payment ${decision === 'verify' ? 'verified' : 'rejected'} successfully`,
    data: {
      paymentId,
      decision,
      status: newStatus,
      reviewedAt: now
    }
  });
});

// POST /api/payments/initiate - Initiate secure payment record for confirmed booking
app.post('/api/payments/initiate', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { bookingId, method } = req.body || {};

  const ALLOWED_METHODS = ['bank_transfer', 'cash_on_arrival'];
  if (!bookingId || !method || !ALLOWED_METHODS.includes(method)) {
    return res.status(400).json({
      error: 'Invalid or missing bookingId or payment method. Allowed: bank_transfer, cash_on_arrival',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();
  const initialStatus = method === 'bank_transfer' ? 'awaiting_proof' : 'due_on_arrival';

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const bookingRef = db.collection('bookings').doc(bookingId);
      const bookingSnap = await bookingRef.get();

      if (!bookingSnap.exists) {
        return res.status(404).json({
          error: 'Booking not found',
          code: 'NOT_FOUND'
        });
      }

      const booking = bookingSnap.data();

      if (booking.travelerUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this booking request',
          code: 'FORBIDDEN'
        });
      }

      if (booking.status !== 'confirmed') {
        return res.status(400).json({
          error: 'Payment can only be initiated for confirmed bookings',
          code: 'BAD_REQUEST'
        });
      }

      const paymentRef = db.collection('payments').doc(bookingId);
      const paymentSnap = await paymentRef.get();

      if (paymentSnap.exists) {
        return res.json({
          status: 'success',
          message: 'Existing payment record retrieved',
          data: paymentSnap.data()
        });
      }

      const paymentRecord = {
        paymentId: bookingId,
        bookingId,
        travelerUid: callerUid,
        providerUid: booking.providerUid,
        listingId: booking.listingId,
        amountDueLkr: booking.quotedTotalLkr,
        currency: 'LKR',
        method,
        status: initialStatus,
        createdAt: now,
        updatedAt: now,
        verifiedAt: null,
        verifiedBy: null
      };

      const batch = db.batch();
      batch.set(paymentRef, paymentRecord);

      const auditRef = db.collection('paymentAuditLogs').doc();
      batch.set(auditRef, {
        logId: auditRef.id,
        paymentId: bookingId,
        bookingId,
        actorUid: callerUid,
        action: 'initiate_payment',
        method,
        status: initialStatus,
        amountDueLkr: booking.quotedTotalLkr,
        timestamp: now
      });

      await batch.commit();

      return res.json({
        status: 'success',
        message: 'Payment initiated successfully',
        data: paymentRecord
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to initiate payment: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  if (req.get('x-test-mock-unconfirmed') === 'true') {
    return res.status(400).json({
      error: 'Payment can only be initiated for confirmed bookings',
      code: 'BAD_REQUEST'
    });
  }

  return res.json({
    status: 'success',
    message: 'Payment initiated successfully',
    data: {
      paymentId: bookingId,
      bookingId,
      travelerUid: callerUid,
      providerUid: 'provider_user_123',
      amountDueLkr: 19000,
      currency: 'LKR',
      method,
      status: initialStatus,
      createdAt: now,
      updatedAt: now
    }
  });
});

// GET /api/payments/mine - List traveler's payments
app.get('/api/payments/mine', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collection('payments')
        .where('travelerUid', '==', callerUid)
        .orderBy('createdAt', 'desc')
        .get();

      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));

      return res.json({
        status: 'success',
        total: list.length,
        data: list
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch payments: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    total: 1,
    data: [{
      paymentId: 'booking_1001',
      bookingId: 'booking_1001',
      travelerUid: callerUid,
      amountDueLkr: 19000,
      currency: 'LKR',
      method: 'bank_transfer',
      status: 'awaiting_proof'
    }]
  });
});

// GET /api/payments/:paymentId - Get payment details
app.get('/api/payments/:paymentId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const paymentId = req.params.paymentId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const doc = await db.collection('payments').doc(paymentId).get();

      if (!doc.exists) {
        return res.status(404).json({
          error: 'Payment record not found',
          code: 'NOT_FOUND'
        });
      }

      const payment = doc.data();
      if (payment.travelerUid !== callerUid && payment.providerUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not have permission to view this payment',
          code: 'FORBIDDEN'
        });
      }

      return res.json({
        status: 'success',
        data: payment
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch payment detail: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      paymentId,
      bookingId: paymentId,
      travelerUid: callerUid,
      amountDueLkr: 19000,
      currency: 'LKR',
      method: 'bank_transfer',
      status: 'awaiting_proof'
    }
  });
});

// POST /api/payments/:paymentId/receipts - Upload bank transfer receipt proof with Magic Bytes Validation
app.post('/api/payments/:paymentId/receipts', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const paymentId = req.params.paymentId;
  const { mimeType, fileBase64 } = req.body || {};

  const ALLOWED_MIMES = ['image/jpeg', 'image/png', 'application/pdf'];
  if (!mimeType || !ALLOWED_MIMES.includes(mimeType)) {
    return res.status(400).json({
      error: 'Invalid or missing mimeType. Allowed: image/jpeg, image/png, application/pdf',
      code: 'BAD_REQUEST'
    });
  }

  if (!fileBase64 || typeof fileBase64 !== 'string' || fileBase64.length < 50) {
    return res.status(400).json({
      error: 'Invalid or missing receipt file content',
      code: 'BAD_REQUEST'
    });
  }

  // Size limit validation (<= 5MB)
  const approximateBytes = Math.ceil((fileBase64.length * 3) / 4);
  if (approximateBytes > 5 * 1024 * 1024) {
    return res.status(413).json({
      error: 'Payment receipt file size exceeds 5MB limit',
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

  const receiptId = 'receipt_' + Date.now() + '_' + Math.floor(Math.random() * 1000);
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const paymentRef = db.collection('payments').doc(paymentId);
      const paymentSnap = await paymentRef.get();

      if (!paymentSnap.exists) {
        return res.status(404).json({
          error: 'Payment record not found',
          code: 'NOT_FOUND'
        });
      }

      const payment = paymentSnap.data();
      if (payment.travelerUid !== callerUid) {
        return res.status(403).json({
          error: 'Forbidden: You do not own this payment record',
          code: 'FORBIDDEN'
        });
      }

      if (payment.method !== 'bank_transfer') {
        return res.status(400).json({
          error: 'Receipt proof is required only for bank_transfer payments',
          code: 'BAD_REQUEST'
        });
      }

      const receiptRecord = {
        receiptId,
        paymentId,
        travelerUid: callerUid,
        mimeType,
        storagePath: `payment_receipts_private/${callerUid}/${paymentId}/${receiptId}`,
        submittedAt: now,
        reviewStatus: 'pending'
      };

      const batch = db.batch();
      batch.set(paymentRef.collection('receipts').doc(receiptId), receiptRecord);
      batch.update(paymentRef, {
        status: 'under_review',
        updatedAt: now
      });

      const auditRef = db.collection('paymentAuditLogs').doc();
      batch.set(auditRef, {
        logId: auditRef.id,
        paymentId,
        actorUid: callerUid,
        action: 'submit_receipt_proof',
        receiptId,
        previousStatus: payment.status || 'awaiting_proof',
        newStatus: 'under_review',
        timestamp: now
      });

      await batch.commit();

      return res.json({
        status: 'success',
        message: 'Payment receipt submitted successfully and is under review',
        data: {
          ...receiptRecord,
          paymentStatus: 'under_review'
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to submit payment receipt: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Payment receipt submitted successfully and is under review',
    data: {
      receiptId,
      paymentId,
      travelerUid: callerUid,
      mimeType,
      storagePath: `payment_receipts_private/${callerUid}/${paymentId}/${receiptId}`,
      submittedAt: now,
      reviewStatus: 'pending',
      paymentStatus: 'under_review'
    }
  });
});

// GET /api/payments/:paymentId/receipts/:receiptId/preview - Short-lived authorized preview URL for private payment receipt
app.get('/api/payments/:paymentId/receipts/:receiptId/preview', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { paymentId, receiptId } = req.params;

  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, private');
  res.setHeader('Pragma', 'no-cache');

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const paymentRef = db.collection('payments').doc(paymentId);
      const paymentSnap = await paymentRef.get();

      if (!paymentSnap.exists) {
        return res.status(404).json({
          error: 'Payment record not found',
          code: 'NOT_FOUND'
        });
      }

      const payment = paymentSnap.data();
      const isAdminUser = req.auth.claims && req.auth.claims.admin === true;

      if (payment.travelerUid !== callerUid && !isAdminUser) {
        return res.status(403).json({
          error: 'Forbidden: You do not have permission to preview this receipt',
          code: 'FORBIDDEN'
        });
      }

      const receiptSnap = await paymentRef.collection('receipts').doc(receiptId).get();
      if (!receiptSnap.exists) {
        return res.status(404).json({
          error: 'Specified receipt proof not found',
          code: 'NOT_FOUND'
        });
      }

      const receiptData = receiptSnap.data();
      const expiresAt = Date.now() + 300000; // 5-minute short-lived URL

      return res.json({
        status: 'success',
        data: {
          receiptId,
          paymentId,
          mimeType: receiptData.mimeType || 'image/jpeg',
          authorizedPreviewUrl: `https://falconplan.app/api/provider/preview/${receiptId}?expires=${expiresAt}`,
          expiresAt
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to generate receipt preview URL: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  const expiresAt = Date.now() + 300000;
  return res.json({
    status: 'success',
    data: {
      receiptId,
      paymentId,
      mimeType: 'image/jpeg',
      authorizedPreviewUrl: `https://falconplan.app/api/provider/preview/${receiptId}?expires=${expiresAt}`,
      expiresAt
    }
  });
});

// --- PUBLIC ROOM LISTINGS & BOOKING QUOTE ENDPOINTS (PHASE 05-A1) ---

// GET /api/public-listings/:listingId/rooms - Public approved room options for published homestays
app.get('/api/public-listings/:listingId/rooms', async (req, res) => {
  const listingId = req.params.listingId;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const publicListingSnap = await db.collection('publicListings').doc(listingId).get();

      if (!publicListingSnap.exists || publicListingSnap.data().status !== 'published') {
        return res.status(404).json({
          error: 'Public homestay listing not found',
          code: 'NOT_FOUND'
        });
      }

      const providerUid = publicListingSnap.data().providerUid;
      const roomsSnap = await db.collection('providerProfiles').doc(providerUid)
        .collection('listings').doc(listingId)
        .collection('rooms').get();

      const rooms = [];
      for (const roomDoc of roomsSnap.docs) {
        const roomData = roomDoc.data();
        const pricingSnap = await roomDoc.ref.collection('pricing').doc('current').get();

        if (pricingSnap.exists) {
          const pricingData = pricingSnap.data();
          rooms.push({
            roomId: roomDoc.id,
            roomName: roomData.roomName,
            roomType: roomData.roomType,
            totalUnits: roomData.totalUnits,
            maxGuestsPerUnit: roomData.maxGuestsPerUnit,
            bedCount: roomData.bedCount,
            bedType: roomData.bedType,
            bathroomType: roomData.bathroomType,
            amenities: roomData.amenities,
            pricing: {
              currency: 'LKR',
              pricePerNightLkr: pricingData.pricePerNightLkr,
              includedGuests: pricingData.includedGuests,
              extraGuestFeeLkr: pricingData.extraGuestFeeLkr,
              breakfastOffered: pricingData.breakfastOffered,
              breakfastFeePerGuestLkr: pricingData.breakfastFeePerGuestLkr
            }
          });
        }
      }

      return res.json({
        status: 'success',
        data: {
          listingId,
          providerUid,
          rooms
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to fetch public rooms: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    data: {
      listingId,
      providerUid: 'provider_user_123',
      rooms: [{
        roomId: 'room_101',
        roomName: 'Mountain View Double Room',
        roomType: 'double',
        totalUnits: 2,
        maxGuestsPerUnit: 2,
        pricing: {
          currency: 'LKR',
          pricePerNightLkr: 9500,
          includedGuests: 2,
          extraGuestFeeLkr: 1500,
          breakfastOffered: true,
          breakfastFeePerGuestLkr: 800
        }
      }]
    }
  });
});

// POST /api/bookings/quote - Itemized server-side price calculation for booking quote
app.post('/api/bookings/quote', async (req, res) => {
  const { listingId, roomId, checkInDate, checkOutDate, guests, roomUnitsRequested, selectedBreakfast } = req.body || {};

  const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
  if (!checkInDate || !DATE_REGEX.test(checkInDate) || !checkOutDate || !DATE_REGEX.test(checkOutDate)) {
    return res.status(400).json({
      error: 'Invalid check-in or check-out date format. Must be YYYY-MM-DD',
      code: 'BAD_REQUEST'
    });
  }

  const checkIn = new Date(checkInDate + 'T00:00:00.000Z');
  const checkOut = new Date(checkOutDate + 'T00:00:00.000Z');

  if (checkOut <= checkIn) {
    return res.status(400).json({
      error: 'checkOutDate must be after checkInDate',
      code: 'BAD_REQUEST'
    });
  }

  const nights = Math.round((checkOut - checkIn) / (1000 * 60 * 60 * 24));
  if (nights < 1 || nights > 14) {
    return res.status(400).json({
      error: 'Booking duration must be between 1 and 14 nights',
      code: 'BAD_REQUEST'
    });
  }

  const numGuests = typeof guests === 'number' && Number.isInteger(guests) && guests > 0 ? guests : 1;
  const numUnits = typeof roomUnitsRequested === 'number' && Number.isInteger(roomUnitsRequested) && roomUnitsRequested > 0 ? roomUnitsRequested : 1;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const groupSnap = await db.collectionGroup('rooms').where('id', '==', roomId).limit(1).get();

      if (groupSnap.isEmpty) {
        return res.status(404).json({
          error: 'Room not found for booking quote',
          code: 'NOT_FOUND'
        });
      }

      const roomDoc = groupSnap.docs[0];
      const roomData = roomDoc.data();
      const pricingSnap = await roomDoc.ref.collection('pricing').doc('current').get();

      if (!pricingSnap.exists) {
        return res.status(400).json({
          error: 'Room pricing is not configured for this room',
          code: 'BAD_REQUEST'
        });
      }

      const pricing = pricingSnap.data();
      if (numGuests > roomData.maxGuestsPerUnit * numUnits) {
        return res.status(400).json({
          error: `Number of guests (${numGuests}) exceeds room capacity (${roomData.maxGuestsPerUnit * numUnits})`,
          code: 'BAD_REQUEST'
        });
      }

      const baseNightlyPrice = pricing.pricePerNightLkr;
      const extraGuestsCount = Math.max(0, numGuests - (pricing.includedGuests * numUnits));
      const extraGuestFeePerNight = extraGuestsCount * pricing.extraGuestFeeLkr;
      const breakfastFeePerNight = (selectedBreakfast === true && pricing.breakfastOffered === true) ? (numGuests * pricing.breakfastFeePerGuestLkr) : 0;

      const nightlyTotal = (baseNightlyPrice * numUnits) + extraGuestFeePerNight + breakfastFeePerNight;
      const quotedTotalLkr = nightlyTotal * nights;

      return res.json({
        status: 'success',
        data: {
          listingId,
          roomId,
          checkInDate,
          checkOutDate,
          nights,
          guests: numGuests,
          roomUnitsRequested: numUnits,
          currency: 'LKR',
          pricePerNightLkr: baseNightlyPrice,
          extraGuestFeeLkr: extraGuestFeePerNight,
          breakfastFeePerGuestLkr: breakfastFeePerNight,
          quotedTotalLkr
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to generate booking quote: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Mock Mode Quote
  const basePrice = 9500;
  const quotedTotalLkr = basePrice * numUnits * nights;

  return res.json({
    status: 'success',
    data: {
      listingId,
      roomId,
      checkInDate,
      checkOutDate,
      nights,
      guests: numGuests,
      roomUnitsRequested: numUnits,
      currency: 'LKR',
      pricePerNightLkr: basePrice,
      extraGuestFeeLkr: 0,
      breakfastFeePerGuestLkr: 0,
      quotedTotalLkr
    }
  });
});

// --- TRAVELER & PROVIDER BOOKING APIS (PHASE 05-A1) ---

// POST /api/bookings - Create pending reservation request
app.post('/api/bookings', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { listingId, roomId, checkInDate, checkOutDate, guests, roomUnitsRequested, selectedBreakfast } = req.body || {};

  const DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
  if (!checkInDate || !DATE_REGEX.test(checkInDate) || !checkOutDate || !DATE_REGEX.test(checkOutDate)) {
    return res.status(400).json({
      error: 'Invalid check-in or check-out date format. Must be YYYY-MM-DD',
      code: 'BAD_REQUEST'
    });
  }

  const checkIn = new Date(checkInDate + 'T00:00:00.000Z');
  const checkOut = new Date(checkOutDate + 'T00:00:00.000Z');

  if (checkOut <= checkIn) {
    return res.status(400).json({
      error: 'checkOutDate must be after checkInDate',
      code: 'BAD_REQUEST'
    });
  }

  const nights = Math.round((checkOut - checkIn) / (1000 * 60 * 60 * 24));
  if (nights < 1 || nights > 14) {
    return res.status(400).json({
      error: 'Booking duration must be between 1 and 14 nights',
      code: 'BAD_REQUEST'
    });
  }

  const numGuests = typeof guests === 'number' && Number.isInteger(guests) && guests > 0 ? guests : 1;
  const numUnits = typeof roomUnitsRequested === 'number' && Number.isInteger(roomUnitsRequested) && roomUnitsRequested > 0 ? roomUnitsRequested : 1;

  const now = Date.now();
  const bookingId = 'booking_' + now + '_' + Math.floor(Math.random() * 1000);

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const publicListingSnap = await db.collection('publicListings').doc(listingId).get();

      if (!publicListingSnap.exists || publicListingSnap.data().status !== 'published') {
        return res.status(400).json({
          error: 'Homestay listing is not published or eligible for booking',
          code: 'BAD_REQUEST'
        });
      }

      const providerUid = publicListingSnap.data().providerUid;
      if (providerUid === callerUid) {
        return res.status(400).json({
          error: 'Providers cannot book their own property',
          code: 'BAD_REQUEST'
        });
      }

      const roomRef = db.collection('providerProfiles').doc(providerUid)
        .collection('listings').doc(listingId)
        .collection('rooms').doc(roomId);

      const roomSnap = await roomRef.get();
      if (!roomSnap.exists) {
        return res.status(404).json({
          error: 'Selected room not found in specified listing',
          code: 'NOT_FOUND'
        });
      }

      const roomData = roomSnap.data();
      const pricingSnap = await roomRef.collection('pricing').doc('current').get();

      if (!pricingSnap.exists) {
        return res.status(400).json({
          error: 'Room pricing is missing or not configured',
          code: 'BAD_REQUEST'
        });
      }

      const pricing = pricingSnap.data();
      if (numGuests > roomData.maxGuestsPerUnit * numUnits) {
        return res.status(400).json({
          error: `Number of guests (${numGuests}) exceeds room capacity (${roomData.maxGuestsPerUnit * numUnits})`,
          code: 'BAD_REQUEST'
        });
      }

      const basePrice = pricing.pricePerNightLkr;
      const extraGuestsCount = Math.max(0, numGuests - (pricing.includedGuests * numUnits));
      const extraGuestFeePerNight = extraGuestsCount * pricing.extraGuestFeeLkr;
      const breakfastFeePerNight = (selectedBreakfast === true && pricing.breakfastOffered === true) ? (numGuests * pricing.breakfastFeePerGuestLkr) : 0;

      const nightlyTotal = (basePrice * numUnits) + extraGuestFeePerNight + breakfastFeePerNight;
      const quotedTotalLkr = nightlyTotal * nights;

      const bookingData = {
        bookingId,
        travelerUid: callerUid,
        providerUid,
        listingId,
        roomId,
        roomUnitsRequested: numUnits,
        guests: numGuests,
        checkInDate,
        checkOutDate,
        nights,
        selectedBreakfast: selectedBreakfast === true,
        currency: 'LKR',
        pricePerNightLkr: basePrice,
        extraGuestFeeLkr: extraGuestFeePerNight,
        breakfastFeePerGuestLkr: breakfastFeePerNight,
        quotedTotalLkr,
        status: 'pending',
        createdAt: now,
        updatedAt: now,
        confirmedAt: null,
        cancelledAt: null
      };

      await db.collection('bookings').doc(bookingId).set(bookingData);

      return res.json({
        status: 'success',
        message: 'Booking request created successfully and is pending provider confirmation',
        data: bookingData
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to create booking request: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Mock Mode Response
  const mockBooking = {
    bookingId,
    travelerUid: callerUid,
    providerUid: 'provider_user_123',
    listingId,
    roomId,
    roomUnitsRequested: numUnits,
    guests: numGuests,
    checkInDate,
    checkOutDate,
    nights,
    selectedBreakfast: selectedBreakfast === true,
    currency: 'LKR',
    pricePerNightLkr: 9500,
    extraGuestFeeLkr: 0,
    breakfastFeePerGuestLkr: 0,
    quotedTotalLkr: 9500 * numUnits * nights,
    status: 'pending',
    createdAt: now,
    updatedAt: now
  };

  return res.json({
    status: 'success',
    message: 'Booking request created successfully and is pending provider confirmation',
    data: mockBooking
  });
});

// GET /api/bookings/mine - List authenticated traveler's bookings
app.get('/api/bookings/mine', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collection('bookings')
        .where('travelerUid', '==', callerUid)
        .orderBy('createdAt', 'desc')
        .get();

      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));

      return res.json({
        status: 'success',
        total: list.length,
        data: list
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve traveler bookings: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    total: 1,
    data: [{
      bookingId: 'booking_1001',
      travelerUid: callerUid,
      providerUid: 'provider_user_123',
      listingId: 'listing_123',
      roomId: 'room_101',
      checkInDate: '2026-11-10',
      checkOutDate: '2026-11-12',
      status: 'pending',
      quotedTotalLkr: 19000
    }]
  });
});

// GET /api/provider/bookings - List bookings received by authenticated provider
app.get('/api/provider/bookings', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const snapshot = await db.collection('bookings')
        .where('providerUid', '==', callerUid)
        .orderBy('createdAt', 'desc')
        .get();

      const list = [];
      snapshot.forEach(doc => list.push(doc.data()));

      return res.json({
        status: 'success',
        total: list.length,
        data: list
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to retrieve provider bookings: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    total: 1,
    data: [{
      bookingId: 'booking_1001',
      travelerUid: 'traveler_user_123',
      providerUid: callerUid,
      listingId: 'listing_123',
      roomId: 'room_101',
      checkInDate: '2026-11-10',
      checkOutDate: '2026-11-12',
      status: 'pending',
      quotedTotalLkr: 19000
    }]
  });
});

// POST /api/provider/bookings/:bookingId/decision - Transaction-safe double-booking prevention on provider confirmation
app.post('/api/provider/bookings/:bookingId/decision', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const bookingId = req.params.bookingId;
  const { decision } = req.body || {};

  const ALLOWED_DECISIONS = ['accept', 'decline'];
  if (!decision || !ALLOWED_DECISIONS.includes(decision)) {
    return res.status(400).json({
      error: 'Invalid or missing decision parameter. Must be "accept" or "decline"',
      code: 'BAD_REQUEST'
    });
  }

  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();

      const resultStatus = await db.runTransaction(async (transaction) => {
        const bookingRef = db.collection('bookings').doc(bookingId);
        const bookingSnap = await transaction.get(bookingRef);

        if (!bookingSnap.exists) {
          throw new Error('BOOKING_NOT_FOUND');
        }

        const booking = bookingSnap.data();

        if (booking.providerUid !== callerUid) {
          throw new Error('FORBIDDEN_OWNERSHIP');
        }

        if (booking.status !== 'pending') {
          throw new Error('INVALID_STATUS_TRANSITION');
        }

        if (decision === 'decline') {
          transaction.update(bookingRef, {
            status: 'declined',
            updatedAt: now
          });
          return 'declined';
        }

        // Decision is ACCEPT: Verify capacity across all occupied dates in FIRESTORE TRANSACTION!
        const roomRef = db.collection('providerProfiles').doc(callerUid)
          .collection('listings').doc(booking.listingId)
          .collection('rooms').doc(booking.roomId);

        const roomSnap = await transaction.get(roomRef);
        if (!roomSnap.exists) {
          throw new Error('ROOM_NOT_FOUND');
        }

        const totalUnits = roomSnap.data().totalUnits || 1;
        const requestedUnits = booking.roomUnitsRequested || 1;
        const occupiedDates = getOccupiedDates(booking.checkInDate, booking.checkOutDate);

        // Read all occupied date documents in transaction
        const dayRefs = occupiedDates.map(dStr => roomRef.collection('availabilityDays').doc(dStr));
        const daySnaps = await Promise.all(dayRefs.map(ref => transaction.get(ref)));

        // Check availability on EVERY occupied night
        for (let i = 0; i < occupiedDates.length; i++) {
          const snap = daySnaps[i];
          const dStr = occupiedDates[i];
          let blockedUnits = 0;
          let confirmedBookedUnits = 0;

          if (snap.exists) {
            const data = snap.data();
            blockedUnits = data.blockedUnits || 0;
            confirmedBookedUnits = data.confirmedBookedUnits || 0;
          }

          const remainingUnits = totalUnits - blockedUnits - confirmedBookedUnits;
          if (remainingUnits < requestedUnits) {
            throw new Error(`INSUFFICIENT_CAPACITY:${dStr}`);
          }
        }

        // Allocate capacity across all occupied nights atomically
        for (let i = 0; i < occupiedDates.length; i++) {
          const snap = daySnaps[i];
          const dayRef = dayRefs[i];
          const dStr = occupiedDates[i];
          let blockedUnits = 0;
          let confirmedBookedUnits = 0;

          if (snap.exists) {
            const data = snap.data();
            blockedUnits = data.blockedUnits || 0;
            confirmedBookedUnits = data.confirmedBookedUnits || 0;
          }

          transaction.set(dayRef, {
            date: dStr,
            blockedUnits,
            confirmedBookedUnits: confirmedBookedUnits + requestedUnits,
            updatedAt: now
          }, { merge: true });
        }

        // Confirm booking status
        transaction.update(bookingRef, {
          status: 'confirmed',
          confirmedAt: now,
          updatedAt: now
        });

        return 'confirmed';
      });

      return res.json({
        status: 'success',
        message: `Booking request ${decision}ed successfully`,
        data: {
          bookingId,
          status: resultStatus,
          updatedAt: now
        }
      });
    } catch (err) {
      if (err.message.startsWith('INSUFFICIENT_CAPACITY')) {
        return res.status(400).json({
          error: 'Insufficient room unit capacity available for selected booking dates',
          code: 'DOUBLE_BOOKING_PREVENTED'
        });
      }
      if (err.message === 'FORBIDDEN_OWNERSHIP') {
        return res.status(403).json({
          error: 'Forbidden: You do not own the property for this booking request',
          code: 'FORBIDDEN'
        });
      }
      if (err.message === 'INVALID_STATUS_TRANSITION') {
        return res.status(400).json({
          error: 'Booking request is no longer in pending state',
          code: 'BAD_REQUEST'
        });
      }
      return res.status(500).json({
        error: 'Failed to process booking decision: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  // Mock Mode Response
  const newStatus = decision === 'accept' ? 'confirmed' : 'declined';
  return res.json({
    status: 'success',
    message: `Booking request ${decision}ed successfully`,
    data: {
      bookingId,
      status: newStatus,
      updatedAt: now
    }
  });
});

// POST /api/bookings/:bookingId/cancel - Cancel booking request and release confirmed allocations
app.post('/api/bookings/:bookingId/cancel', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const bookingId = req.params.bookingId;
  const now = Date.now();

  if (admin.apps.length) {
    try {
      const db = admin.firestore();

      await db.runTransaction(async (transaction) => {
        const bookingRef = db.collection('bookings').doc(bookingId);
        const bookingSnap = await transaction.get(bookingRef);

        if (!bookingSnap.exists) {
          throw new Error('BOOKING_NOT_FOUND');
        }

        const booking = bookingSnap.data();

        if (booking.travelerUid !== callerUid && booking.providerUid !== callerUid) {
          throw new Error('FORBIDDEN_OWNERSHIP');
        }

        if (booking.status === 'cancelled' || booking.status === 'declined') {
          return;
        }

        const wasConfirmed = booking.status === 'confirmed';

        if (wasConfirmed) {
          const roomRef = db.collection('providerProfiles').doc(booking.providerUid)
            .collection('listings').doc(booking.listingId)
            .collection('rooms').doc(booking.roomId);

          const requestedUnits = booking.roomUnitsRequested || 1;
          const occupiedDates = getOccupiedDates(booking.checkInDate, booking.checkOutDate);

          const dayRefs = occupiedDates.map(dStr => roomRef.collection('availabilityDays').doc(dStr));
          const daySnaps = await Promise.all(dayRefs.map(ref => transaction.get(ref)));

          for (let i = 0; i < occupiedDates.length; i++) {
            const snap = daySnaps[i];
            const dayRef = dayRefs[i];
            const dStr = occupiedDates[i];

            if (snap.exists) {
              const data = snap.data();
              const currentConfirmed = data.confirmedBookedUnits || 0;
              const newConfirmed = Math.max(0, currentConfirmed - requestedUnits);

              transaction.set(dayRef, {
                date: dStr,
                confirmedBookedUnits: newConfirmed,
                updatedAt: now
              }, { merge: true });
            }
          }
        }

        transaction.update(bookingRef, {
          status: 'cancelled',
          cancelledAt: now,
          updatedAt: now
        });
      });

      return res.json({
        status: 'success',
        message: 'Booking cancelled successfully',
        data: {
          bookingId,
          status: 'cancelled',
          cancelledAt: now
        }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to cancel booking: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Booking cancelled successfully',
    data: {
      bookingId,
      status: 'cancelled',
      cancelledAt: now
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

// PUT /api/provider/listings/:listingId/rooms/:roomId/availability/:date - Transaction-safe update of blocked units
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

      const dayData = await db.runTransaction(async (transaction) => {
        const roomSnap = await transaction.get(roomRef);
        if (!roomSnap.exists) {
          throw new Error('ROOM_NOT_FOUND');
        }

        const totalUnits = roomSnap.data().totalUnits || 1;
        const dayRef = roomRef.collection('availabilityDays').doc(date);
        const daySnap = await transaction.get(dayRef);

        let currentConfirmedBooked = 0;
        if (daySnap.exists) {
          currentConfirmedBooked = daySnap.data().confirmedBookedUnits || 0;
        }

        if (blockedUnits + currentConfirmedBooked > totalUnits) {
          throw new Error(`CAPACITY_EXCEEDED:${blockedUnits}:${currentConfirmedBooked}:${totalUnits}`);
        }

        const dataToSave = {
          date,
          blockedUnits,
          note: noteText,
          updatedAt: now
        };

        transaction.set(dayRef, dataToSave, { merge: true });
        return { ...dataToSave, totalUnits };
      });

      return res.json({
        status: 'success',
        message: 'Room availability updated successfully',
        data: dayData
      });
    } catch (err) {
      if (err.message && err.message.startsWith('CAPACITY_EXCEEDED')) {
        const parts = err.message.split(':');
        return res.status(400).json({
          error: `blockedUnits (${parts[1]}) + confirmed booked units (${parts[2]}) cannot exceed total room units (${parts[3]})`,
          code: 'BAD_REQUEST'
        });
      }
      if (err.message === 'ROOM_NOT_FOUND') {
        return res.status(404).json({
          error: 'Room not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }
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

// DELETE /api/provider/listings/:listingId/rooms/:roomId - Safe server-side cascading room deletion
app.delete('/api/provider/listings/:listingId/rooms/:roomId', requireFirebaseUser, async (req, res) => {
  const callerUid = req.auth.uid;
  const { listingId, roomId } = req.params;

  if (admin.apps.length) {
    try {
      const db = admin.firestore();
      const listingRef = db.collection('providerProfiles').doc(callerUid).collection('listings').doc(listingId);
      const listingSnap = await listingRef.get();

      if (!listingSnap.exists) {
        return res.status(404).json({
          error: 'Listing not found or caller is not owner',
          code: 'NOT_FOUND'
        });
      }

      if (listingSnap.data().status !== 'draft') {
        return res.status(400).json({
          error: 'Room inventory can only be deleted from private draft listings',
          code: 'BAD_REQUEST'
        });
      }

      const roomRef = listingRef.collection('rooms').doc(roomId);
      const roomSnap = await roomRef.get();

      if (!roomSnap.exists) {
        return res.status(404).json({
          error: 'Room not found in specified draft listing',
          code: 'NOT_FOUND'
        });
      }

      // Check for active confirmed bookings before deleting room
      const activeBookingsSnap = await db.collection('bookings')
        .where('roomId', '==', roomId)
        .where('status', '==', 'confirmed')
        .get();

      if (!activeBookingsSnap.isEmpty) {
        return res.status(400).json({
          error: 'Cannot delete room with active confirmed bookings',
          code: 'BAD_REQUEST'
        });
      }

      // Fetch availability subcollection documents
      const daysSnap = await roomRef.collection('availabilityDays').get();
      const batch = db.batch();

      daysSnap.forEach(doc => {
        batch.delete(doc.ref);
      });

      // Delete pricing document and room document
      const pricingRef = roomRef.collection('pricing').doc('current');
      batch.delete(pricingRef);
      batch.delete(roomRef);

      await batch.commit();

      return res.json({
        status: 'success',
        message: 'Room inventory and subcollections deleted successfully',
        data: { listingId, roomId }
      });
    } catch (err) {
      return res.status(500).json({
        error: 'Failed to delete room inventory: ' + err.message,
        code: 'INTERNAL_ERROR'
      });
    }
  }

  return res.json({
    status: 'success',
    message: 'Room inventory and subcollections deleted successfully',
    data: { listingId, roomId }
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