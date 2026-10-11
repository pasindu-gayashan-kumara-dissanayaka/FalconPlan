'use strict';

const express = require('express');
const helmet = require('helmet');
const { requireFirebaseUser, requireAdmin, requireRole } = require('./auth');

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