#!/usr/bin/env node
/**
 * test.cjs - Cross-platform test execution script
 *
 * This script:
 * - Ensures dependencies are installed via install.cjs
 * - Runs backend tests (JUnit via Gradle)
 * - Runs frontend tests (Jest)
 * - Reports results from both test suites
 */

const { execSync, spawnSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Import paths from install.cjs
const { BACKEND_DIR, FRONTEND_DIR, ROOT_DIR } = require('./install.cjs');

/**
 * Ensure dependencies are installed
 */
function ensureDependencies() {
  console.error('[test] Ensuring dependencies are installed...');

  try {
    execSync('node tools/install.cjs', {
      cwd: ROOT_DIR,
      stdio: 'inherit',
      shell: true
    });
    console.error('[test] Dependencies are ready');
    return true;
  } catch (error) {
    console.error('[test] ERROR: Dependency installation failed');
    return false;
  }
}

/**
 * Run backend tests (JUnit via Gradle)
 */
function runBackendTests() {
  console.error('\n[test] ========================================');
  console.error('[test] Running Backend Tests (JUnit)');
  console.error('[test] ========================================');

  if (!fs.existsSync(BACKEND_DIR)) {
    console.error('[test] Backend directory not found, skipping backend tests');
    return true;
  }

  const gradlewCmd = process.platform === 'win32'
    ? 'gradlew.bat'
    : './gradlew';

  const gradlewPath = path.join(BACKEND_DIR, gradlewCmd);

  if (!fs.existsSync(gradlewPath)) {
    console.error('[test] ERROR: Gradle wrapper not found at', gradlewPath);
    return false;
  }

  // Check if test directory exists
  const testDir = path.join(BACKEND_DIR, 'src', 'test', 'java');
  if (!fs.existsSync(testDir)) {
    console.error('[test] No backend test directory found, skipping backend tests');
    console.error('[test] (Backend tests would be in src/test/java)');
    return true;
  }

  try {
    console.error('[test] Executing backend tests...');
    execSync(
      `${gradlewCmd} test`,
      {
        cwd: BACKEND_DIR,
        stdio: 'inherit',
        shell: true
      }
    );
    console.error('[test] Backend tests passed successfully');
    return true;
  } catch (error) {
    console.error('[test] Backend tests failed');
    console.error('[test] Check test reports at:', path.join(BACKEND_DIR, 'build/reports/tests/test/index.html'));
    return false;
  }
}

/**
 * Run frontend tests (Jest)
 */
function runFrontendTests() {
  console.error('\n[test] ========================================');
  console.error('[test] Running Frontend Tests (Jest)');
  console.error('[test] ========================================');

  if (!fs.existsSync(FRONTEND_DIR)) {
    console.error('[test] Frontend directory not found, skipping frontend tests');
    return true;
  }

  const packageJsonPath = path.join(FRONTEND_DIR, 'package.json');

  if (!fs.existsSync(packageJsonPath)) {
    console.error('[test] ERROR: package.json not found in frontend directory');
    return false;
  }

  // Check for test script in package.json
  let packageJson;
  try {
    packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
  } catch (error) {
    console.error('[test] ERROR: Failed to parse package.json:', error.message);
    return false;
  }

  if (!packageJson.scripts || !packageJson.scripts.test) {
    console.error('[test] No test script found in package.json, skipping frontend tests');
    return true;
  }

  // Check if there are test files
  const srcDir = path.join(FRONTEND_DIR, 'src');
  if (!fs.existsSync(srcDir)) {
    console.error('[test] Frontend source directory not found, skipping frontend tests');
    return true;
  }

  // Look for test files
  let hasTestFiles = false;
  try {
    const files = execSync(
      'find src -type f \\( -name "*.test.ts" -o -name "*.test.tsx" -o -name "*.spec.ts" -o -name "*.spec.tsx" \\)',
      {
        cwd: FRONTEND_DIR,
        encoding: 'utf8',
        shell: true
      }
    ).trim();

    hasTestFiles = files.length > 0;
  } catch (error) {
    // find command failed or no files found
    hasTestFiles = false;
  }

  if (!hasTestFiles) {
    console.error('[test] No frontend test files found, skipping frontend tests');
    console.error('[test] (Frontend tests should be named *.test.ts, *.test.tsx, *.spec.ts, or *.spec.tsx)');
    return true;
  }

  try {
    console.error('[test] Executing frontend tests...');
    execSync(
      'npm test -- --watchAll=false --passWithNoTests',
      {
        cwd: FRONTEND_DIR,
        stdio: 'inherit',
        shell: true
      }
    );
    console.error('[test] Frontend tests passed successfully');
    return true;
  } catch (error) {
    console.error('[test] Frontend tests failed');
    return false;
  }
}

/**
 * Main test routine
 */
function main() {
  console.error('[test] CBR Viewer - Test Runner');
  console.error('[test] Platform:', process.platform);
  console.error('[test] Node version:', process.version);

  // Ensure dependencies are installed
  if (!ensureDependencies()) {
    console.error('[test] ERROR: Cannot run tests without dependencies');
    process.exit(1);
  }

  let allTestsPassed = true;

  // Run backend tests
  if (!runBackendTests()) {
    allTestsPassed = false;
  }

  // Run frontend tests
  if (!runFrontendTests()) {
    allTestsPassed = false;
  }

  if (allTestsPassed) {
    console.error('\n[test] ========================================');
    console.error('[test] All tests passed successfully!');
    console.error('[test] ========================================\n');
    process.exit(0);
  } else {
    console.error('\n[test] ========================================');
    console.error('[test] Some tests failed');
    console.error('[test] ========================================\n');
    process.exit(1);
  }
}

// Run if executed directly
if (require.main === module) {
  main();
}

module.exports = {
  runBackendTests,
  runFrontendTests
};
