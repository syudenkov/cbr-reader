#!/usr/bin/env node
/**
 * run.cjs - Cross-platform project execution script
 *
 * This script:
 * - Ensures dependencies are installed via install.cjs
 * - Starts the backend (Spring Boot) server
 * - Starts the frontend (Vite) dev server
 * - Manages both processes concurrently
 */

const { spawn, execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Import paths from install.cjs
const { BACKEND_DIR, FRONTEND_DIR } = require('./install.cjs');

/**
 * Ensure dependencies are installed
 */
function ensureDependencies() {
  console.error('[run] Checking dependencies...');

  try {
    execSync('node tools/install.cjs', {
      cwd: path.resolve(__dirname, '..'),
      stdio: 'inherit',
      shell: true
    });
    console.error('[run] Dependencies are ready');
    return true;
  } catch (error) {
    console.error('[run] ERROR: Dependency installation failed');
    return false;
  }
}

/**
 * Start the backend server (Spring Boot via Gradle)
 */
function startBackend() {
  console.error('\n[run] ========================================');
  console.error('[run] Starting Backend Server (Spring Boot)');
  console.error('[run] ========================================');

  if (!fs.existsSync(BACKEND_DIR)) {
    console.error('[run] Backend directory not found, skipping backend startup');
    return null;
  }

  const gradlewCmd = process.platform === 'win32'
    ? 'gradlew.bat'
    : './gradlew';

  const gradlewPath = path.join(BACKEND_DIR, gradlewCmd);

  if (!fs.existsSync(gradlewPath)) {
    console.error('[run] ERROR: Gradle wrapper not found at', gradlewPath);
    return null;
  }

  // Use bootRun to start Spring Boot application
  const backendProcess = spawn(
    gradlewCmd,
    ['bootRun', '--quiet'],
    {
      cwd: BACKEND_DIR,
      stdio: 'inherit',
      shell: true
    }
  );

  backendProcess.on('error', (error) => {
    console.error('[run] Backend process error:', error);
  });

  backendProcess.on('exit', (code, signal) => {
    if (code !== null) {
      console.error(`[run] Backend process exited with code ${code}`);
    } else {
      console.error(`[run] Backend process killed with signal ${signal}`);
    }
  });

  console.error('[run] Backend server starting on http://localhost:8080');

  return backendProcess;
}

/**
 * Start the frontend dev server (Vite)
 */
function startFrontend() {
  console.error('\n[run] ========================================');
  console.error('[run] Starting Frontend Dev Server (Vite)');
  console.error('[run] ========================================');

  if (!fs.existsSync(FRONTEND_DIR)) {
    console.error('[run] Frontend directory not found, skipping frontend startup');
    return null;
  }

  const packageJsonPath = path.join(FRONTEND_DIR, 'package.json');

  if (!fs.existsSync(packageJsonPath)) {
    console.error('[run] ERROR: package.json not found in frontend directory');
    return null;
  }

  // Check for dev script in package.json
  let packageJson;
  try {
    packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
  } catch (error) {
    console.error('[run] ERROR: Failed to parse package.json:', error.message);
    return null;
  }

  if (!packageJson.scripts || !packageJson.scripts.dev) {
    console.error('[run] ERROR: No "dev" script found in package.json');
    return null;
  }

  const frontendProcess = spawn(
    'npm',
    ['run', 'dev'],
    {
      cwd: FRONTEND_DIR,
      stdio: 'inherit',
      shell: true
    }
  );

  frontendProcess.on('error', (error) => {
    console.error('[run] Frontend process error:', error);
  });

  frontendProcess.on('exit', (code, signal) => {
    if (code !== null) {
      console.error(`[run] Frontend process exited with code ${code}`);
    } else {
      console.error(`[run] Frontend process killed with signal ${signal}`);
    }
  });

  console.error('[run] Frontend dev server starting on http://localhost:5173');

  return frontendProcess;
}

/**
 * Main execution routine
 */
function main() {
  console.error('[run] CBR Viewer - Project Runner');
  console.error('[run] Platform:', process.platform);
  console.error('[run] Node version:', process.version);

  // Ensure dependencies are installed
  if (!ensureDependencies()) {
    console.error('[run] ERROR: Cannot start application without dependencies');
    process.exit(1);
  }

  const processes = [];

  // Start backend
  const backendProcess = startBackend();
  if (backendProcess) {
    processes.push(backendProcess);
  }

  // Give backend a moment to start before frontend
  setTimeout(() => {
    // Start frontend
    const frontendProcess = startFrontend();
    if (frontendProcess) {
      processes.push(frontendProcess);
    }

    if (processes.length === 0) {
      console.error('[run] ERROR: No processes were started');
      process.exit(1);
    }

    console.error('\n[run] ========================================');
    console.error('[run] Application is running!');
    console.error('[run] Backend:  http://localhost:8080');
    console.error('[run] Frontend: http://localhost:5173');
    console.error('[run] Press Ctrl+C to stop all services');
    console.error('[run] ========================================\n');
  }, 2000);

  // Handle termination signals
  const cleanup = (signal) => {
    console.error(`\n[run] Received ${signal}, shutting down gracefully...`);

    processes.forEach((proc, index) => {
      if (proc && !proc.killed) {
        console.error(`[run] Stopping process ${index + 1}...`);
        proc.kill('SIGTERM');

        // Force kill after 5 seconds if still running
        setTimeout(() => {
          if (!proc.killed) {
            console.error(`[run] Force killing process ${index + 1}...`);
            proc.kill('SIGKILL');
          }
        }, 5000);
      }
    });

    // Exit after giving processes time to clean up
    setTimeout(() => {
      console.error('[run] Shutdown complete');
      process.exit(0);
    }, 6000);
  };

  process.on('SIGINT', () => cleanup('SIGINT'));
  process.on('SIGTERM', () => cleanup('SIGTERM'));

  // Keep the process running
  process.stdin.resume();
}

// Run if executed directly
if (require.main === module) {
  main();
}

module.exports = {
  startBackend,
  startFrontend
};
