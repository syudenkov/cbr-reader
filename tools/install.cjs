#!/usr/bin/env node
/**
 * install.cjs - Cross-platform dependency installation and environment setup
 *
 * This script handles:
 * - Backend (Java/Gradle) dependency resolution and build setup
 * - Frontend (Node.js/npm) dependency installation
 * - Idempotent execution (safe to run multiple times)
 */

const { execSync, spawnSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// Project directories
const ROOT_DIR = path.resolve(__dirname, '..');
const BACKEND_DIR = path.join(ROOT_DIR, 'backend');
const FRONTEND_DIR = path.join(ROOT_DIR, 'frontend');

/**
 * Execute a command with proper error handling
 */
function executeCommand(command, options = {}) {
  const defaultOptions = {
    cwd: options.cwd || ROOT_DIR,
    stdio: options.silent ? 'ignore' : 'inherit',
    shell: true,
    encoding: 'utf8'
  };

  try {
    console.error(`[install] Executing: ${command} in ${defaultOptions.cwd}`);
    execSync(command, { ...defaultOptions, ...options });
    return true;
  } catch (error) {
    console.error(`[install] Error executing: ${command}`);
    console.error(`[install] Exit code: ${error.status}`);
    if (!options.silent && error.stdout) {
      console.error(error.stdout);
    }
    if (!options.silent && error.stderr) {
      console.error(error.stderr);
    }
    return false;
  }
}

/**
 * Check if a command is available
 */
function commandExists(command) {
  const checkCmd = process.platform === 'win32'
    ? `where ${command}`
    : `which ${command}`;

  try {
    execSync(checkCmd, { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

/**
 * Install backend dependencies (Gradle)
 */
function installBackend() {
  console.error('\n[install] ========================================');
  console.error('[install] Installing Backend Dependencies (Java/Gradle)');
  console.error('[install] ========================================');

  if (!fs.existsSync(BACKEND_DIR)) {
    console.error('[install] Backend directory not found, skipping...');
    return true;
  }

  // Check for Java
  if (!commandExists('java')) {
    console.error('[install] ERROR: Java is not installed or not in PATH');
    console.error('[install] Please install Java 21 or higher');
    return false;
  }

  // Use Gradle wrapper (cross-platform)
  const gradlewCmd = process.platform === 'win32'
    ? 'gradlew.bat'
    : './gradlew';

  const gradlewPath = path.join(BACKEND_DIR, gradlewCmd);

  if (!fs.existsSync(gradlewPath)) {
    console.error('[install] ERROR: Gradle wrapper not found at', gradlewPath);
    return false;
  }

  // Make gradlew executable on Unix systems
  if (process.platform !== 'win32') {
    try {
      fs.chmodSync(gradlewPath, '755');
    } catch (error) {
      console.error('[install] Warning: Could not make gradlew executable:', error.message);
    }
  }

  // Build and resolve dependencies
  // Using --quiet to reduce noise, but showing errors
  const success = executeCommand(
    `${gradlewCmd} build -x test --quiet`,
    { cwd: BACKEND_DIR }
  );

  if (!success) {
    console.error('[install] Backend dependency installation failed');
    return false;
  }

  console.error('[install] Backend dependencies installed successfully');
  return true;
}

/**
 * Install frontend dependencies (npm)
 */
function installFrontend() {
  console.error('\n[install] ========================================');
  console.error('[install] Installing Frontend Dependencies (Node.js/npm)');
  console.error('[install] ========================================');

  if (!fs.existsSync(FRONTEND_DIR)) {
    console.error('[install] Frontend directory not found, skipping...');
    return true;
  }

  // Check for Node.js and npm
  if (!commandExists('node')) {
    console.error('[install] ERROR: Node.js is not installed or not in PATH');
    return false;
  }

  if (!commandExists('npm')) {
    console.error('[install] ERROR: npm is not installed or not in PATH');
    return false;
  }

  const packageJsonPath = path.join(FRONTEND_DIR, 'package.json');
  const nodeModulesPath = path.join(FRONTEND_DIR, 'node_modules');
  const packageLockPath = path.join(FRONTEND_DIR, 'package-lock.json');

  if (!fs.existsSync(packageJsonPath)) {
    console.error('[install] ERROR: package.json not found in frontend directory');
    return false;
  }

  // Check if we need to install (idempotent check)
  let needsInstall = false;

  if (!fs.existsSync(nodeModulesPath)) {
    console.error('[install] node_modules not found, fresh install needed');
    needsInstall = true;
  } else if (fs.existsSync(packageLockPath)) {
    // Check if package-lock.json is newer than node_modules
    const lockStat = fs.statSync(packageLockPath);
    const nodeModulesStat = fs.statSync(nodeModulesPath);

    if (lockStat.mtime > nodeModulesStat.mtime) {
      console.error('[install] package-lock.json is newer, reinstall needed');
      needsInstall = true;
    }
  }

  if (needsInstall) {
    // Use npm ci for clean, reproducible installs if package-lock exists
    const installCmd = fs.existsSync(packageLockPath)
      ? 'npm ci'
      : 'npm install';

    const success = executeCommand(installCmd, { cwd: FRONTEND_DIR });

    if (!success) {
      console.error('[install] Frontend dependency installation failed');
      return false;
    }
  } else {
    console.error('[install] Frontend dependencies are up to date');
  }

  console.error('[install] Frontend dependencies installed successfully');
  return true;
}

/**
 * Main installation routine
 */
function main() {
  console.error('[install] CBR Viewer - Dependency Installation');
  console.error('[install] Platform:', process.platform);
  console.error('[install] Node version:', process.version);
  console.error('[install] Working directory:', ROOT_DIR);

  let success = true;

  // Install backend dependencies
  if (!installBackend()) {
    success = false;
  }

  // Install frontend dependencies
  if (!installFrontend()) {
    success = false;
  }

  if (success) {
    console.error('\n[install] ========================================');
    console.error('[install] All dependencies installed successfully!');
    console.error('[install] ========================================\n');
    process.exit(0);
  } else {
    console.error('\n[install] ========================================');
    console.error('[install] ERROR: Dependency installation failed');
    console.error('[install] ========================================\n');
    process.exit(1);
  }
}

// Run if executed directly
if (require.main === module) {
  main();
}

// Export functions for use by other scripts
module.exports = {
  installBackend,
  installFrontend,
  executeCommand,
  commandExists,
  ROOT_DIR,
  BACKEND_DIR,
  FRONTEND_DIR
};
