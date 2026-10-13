'use strict';

/**
 * FalconPlan Admin Custom Claim Management Utility
 * Usage: node manageAdminClaim.js <grant|revoke> <targetFirebaseUid>
 */

const admin = require('firebase-admin');
const { initAdmin } = require('../src/auth');

async function main() {
  const args = process.argv.slice(2);
  const action = args[0];
  const targetUid = args[1];

  if (!action || !targetUid || !['grant', 'revoke'].includes(action)) {
    console.error('Usage: node scripts/manageAdminClaim.js <grant|revoke> <targetFirebaseUid>');
    process.exit(1);
  }

  if (!initAdmin()) {
    console.error('Error: Firebase Admin SDK credentials not configured in environment.');
    process.exit(1);
  }

  try {
    const user = await admin.auth().getUser(targetUid);
    console.log(`Found Target User: ${user.uid} (${user.email || 'No Email'})`);

    const existingClaims = user.customClaims || {};
    const newClaims = { ...existingClaims };

    if (action === 'grant') {
      newClaims.admin = true;
      await admin.auth().setCustomUserClaims(targetUid, newClaims);
      console.log(`SUCCESS: Granted admin custom claim to UID: ${targetUid}`);
    } else {
      delete newClaims.admin;
      await admin.auth().setCustomUserClaims(targetUid, newClaims);
      console.log(`SUCCESS: Revoked admin custom claim from UID: ${targetUid}`);
    }
  } catch (err) {
    console.error(`FAILED to ${action} admin claim: ${err.message}`);
    process.exit(1);
  }
}

if (require.main === module) {
  main();
}