'use strict';
const {test} = require('node:test');
const assert = require('node:assert/strict');
test('basic Node runtime supports test runner', () => {
  assert.equal(typeof process.version, 'string');
  assert.ok(process.versions.node);
});
