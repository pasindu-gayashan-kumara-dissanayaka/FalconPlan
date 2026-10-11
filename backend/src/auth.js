'use strict';

const admin = require('firebase-admin');

let bootstrapError = null;

function initAdmin() {
  if (admin.apps.length) return true;

  const rawJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  const path = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  const projectId = process.env.FIREBASE_PROJECT_ID || 'falconplan-88ad9';

  if (!rawJson && !path && !process.env.FIREBASE_AUTH_EMULATOR_HOST) {
    bootstrapError = 'Firebase Admin credentials are not configured';
    return false;
  }

  try {
    let credential;
    if (rawJson) {
      credential = admin.credential.cert(JSON.parse(rawJson));
    } else if (path) {
      credential = admin.credential.applicationDefault();
    } else {
      credential = admin.credential.applicationDefault();
    }

    admin.initializeApp({
      credential,
      projectId
    });
    return true;
  } catch (err) {
    bootstrapError = 'Firebase Admin initialization failed';
    return false;
  }
}

async function requireFirebaseUser(req, res, next) {
  // Test / Development stub mode for unit testing without live credentials
  if (process.env.NODE_ENV === 'test' && req.get('x-test-mock-auth') === 'true') {
    const mockUid = req.get('x-test-mock-uid') || 'test_user_uid_123';
    req.auth = {
      uid: mockUid,
      claims: {
        uid: mockUid,
        email: 'testuser@falconplan.lk',
        email_verified: true,
        admin: req.get('x-test-mock-admin') === 'true'
      }
    };
    return next();
  }

  if (!initAdmin()) {
    return res.status(503).json({
      error: 'Server authentication is not configured',
      code: 'SERVICE_UNAVAILABLE'
    });
  }

  const authHeader = req.get('authorization') || '';
  const match = /^Bearer (\S+)$/i.exec(authHeader);

  if (!match) {
    return res.status(401).json({
      error: 'Missing or invalid Authorization Bearer token',
      code: 'UNAUTHORIZED'
    });
  }

  const idToken = match[1];

  try {
    // Verify ID token with checkRevoked set to true for high-security token revocation checks
    const decodedToken = await admin.auth().verifyIdToken(idToken, true);
    req.auth = {
      uid: decodedToken.uid,
      claims: decodedToken
    };
    return next();
  } catch (error) {
    if (error.code === 'auth/id-token-revoked') {
      return res.status(401).json({
        error: 'Firebase ID token has been revoked. Please sign in again.',
        code: 'TOKEN_REVOKED'
      });
    }
    if (error.code === 'auth/id-token-expired') {
      return res.status(401).json({
        error: 'Firebase ID token has expired. Please refresh your session.',
        code: 'TOKEN_EXPIRED'
      });
    }
    return res.status(401).json({
      error: 'Invalid or malformed Firebase ID token',
      code: 'UNAUTHORIZED'
    });
  }
}

function requireAdmin(req, res, next) {
  if (!req.auth || !req.auth.claims || req.auth.claims.admin !== true) {
    return res.status(403).json({
      error: 'Administrator permissions required',
      code: 'FORBIDDEN'
    });
  }
  next();
}

function requireRole(allowedModes) {
  return (req, res, next) => {
    if (!req.auth) {
      return res.status(401).json({
        error: 'Authentication required',
        code: 'UNAUTHORIZED'
      });
    }

    const currentMode = req.auth.claims.activeMode || 'traveler';
    if (Array.isArray(allowedModes) && !allowedModes.includes(currentMode)) {
      return res.status(403).json({
        error: `Insufficient permissions. Account must be in mode: ${allowedModes.join(', ')}`,
        code: 'FORBIDDEN'
      });
    }
    next();
  };
}

module.exports = {
  initAdmin,
  requireFirebaseUser,
  requireAdmin,
  requireRole
};