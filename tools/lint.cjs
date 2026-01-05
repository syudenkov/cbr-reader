#!/usr/bin/env node
/**
 * lint.cjs - Cross-platform code linting script
 *
 * This script:
 * - Ensures dependencies are installed via install.cjs (silent)
 * - Lints backend Java code using Checkstyle
 * - Lints frontend TypeScript/React code using ESLint
 * - Outputs results ONLY as valid JSON to stdout
 * - All diagnostic messages go to stderr
 */

const { execSync } = require('child_process');
const path = require('path');
const fs = require('fs');

// Import paths from install.cjs
const { BACKEND_DIR, FRONTEND_DIR, ROOT_DIR, executeCommand } = require('./install.cjs');

/**
 * Ensure dependencies are installed (silent)
 */
function ensureDependencies() {
  try {
    execSync('node tools/install.cjs', {
      cwd: ROOT_DIR,
      stdio: 'ignore',
      shell: true
    });
    return true;
  } catch (error) {
    console.error('[lint] ERROR: Dependency installation failed');
    return false;
  }
}

/**
 * Install Checkstyle for Java linting if not available
 */
function ensureCheckstyle() {
  const checkstyleDir = path.join(ROOT_DIR, 'tools', 'checkstyle');
  const checkstyleJar = path.join(checkstyleDir, 'checkstyle.jar');

  if (fs.existsSync(checkstyleJar)) {
    console.error('[lint] Checkstyle is already installed');
    return checkstyleJar;
  }

  console.error('[lint] Installing Checkstyle...');

  // Create directory
  if (!fs.existsSync(checkstyleDir)) {
    fs.mkdirSync(checkstyleDir, { recursive: true });
  }

  // Download Checkstyle (using a stable version)
  const checkstyleVersion = '10.12.7';
  const checkstyleUrl = `https://github.com/checkstyle/checkstyle/releases/download/checkstyle-${checkstyleVersion}/checkstyle-${checkstyleVersion}-all.jar`;

  try {
    console.error(`[lint] Downloading Checkstyle ${checkstyleVersion}...`);
    execSync(
      `curl -L -o "${checkstyleJar}" "${checkstyleUrl}"`,
      { stdio: 'inherit', shell: true }
    );
    console.error('[lint] Checkstyle installed successfully');
    return checkstyleJar;
  } catch (error) {
    console.error('[lint] ERROR: Failed to download Checkstyle');
    return null;
  }
}

/**
 * Create default Checkstyle configuration if not exists
 */
function ensureCheckstyleConfig() {
  const configPath = path.join(BACKEND_DIR, 'checkstyle.xml');

  if (fs.existsSync(configPath)) {
    return configPath;
  }

  console.error('[lint] Creating default Checkstyle configuration...');

  const defaultConfig = `<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
  "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
  <property name="severity" value="error"/>

  <module name="TreeWalker">
    <!-- Syntax and Critical Errors Only -->
    <module name="IllegalToken"/>
    <module name="IllegalType"/>
    <module name="OuterTypeFilename"/>
    <module name="PackageDeclaration"/>
    <module name="OneTopLevelClass"/>
  </module>

  <!-- File Tab Character -->
  <module name="FileTabCharacter">
    <property name="severity" value="warning"/>
  </module>
</module>`;

  fs.writeFileSync(configPath, defaultConfig, 'utf8');
  console.error('[lint] Checkstyle configuration created at', configPath);
  return configPath;
}

/**
 * Lint backend Java code using Checkstyle
 */
function lintBackend() {
  console.error('[lint] Linting backend Java code...');

  if (!fs.existsSync(BACKEND_DIR)) {
    console.error('[lint] Backend directory not found, skipping backend linting');
    return [];
  }

  const srcDir = path.join(BACKEND_DIR, 'src', 'main', 'java');
  if (!fs.existsSync(srcDir)) {
    console.error('[lint] Backend source directory not found, skipping backend linting');
    return [];
  }

  const checkstyleJar = ensureCheckstyle();
  if (!checkstyleJar) {
    console.error('[lint] ERROR: Checkstyle is not available');
    return [];
  }

  const configPath = ensureCheckstyleConfig();
  const outputPath = path.join(ROOT_DIR, 'tools', 'checkstyle-result.xml');

  try {
    execSync(
      `java -jar "${checkstyleJar}" -c "${configPath}" -f xml -o "${outputPath}" "${srcDir}"`,
      { stdio: 'ignore', shell: true }
    );
  } catch (error) {
    // Checkstyle exits with non-zero if there are violations
    console.error('[lint] Checkstyle found issues');
  }

  // Parse XML output and convert to JSON
  if (!fs.existsSync(outputPath)) {
    return [];
  }

  const xmlContent = fs.readFileSync(outputPath, 'utf8');
  const errors = parseCheckstyleXml(xmlContent);

  // Clean up temporary file
  try {
    fs.unlinkSync(outputPath);
  } catch (error) {
    // Ignore cleanup errors
  }

  console.error(`[lint] Backend: Found ${errors.length} issue(s)`);
  return errors;
}

/**
 * Parse Checkstyle XML output
 */
function parseCheckstyleXml(xml) {
  const errors = [];
  const fileRegex = /<file name="([^"]+)">/g;
  const errorRegex = /<error line="(\d+)"(?:\s+column="(\d+)")?\s+severity="([^"]+)"\s+message="([^"]+)"\s+source="([^"]+)"/g;

  let currentFile = null;
  const lines = xml.split('\n');

  for (const line of lines) {
    const fileMatch = fileRegex.exec(line);
    if (fileMatch) {
      currentFile = fileMatch[1];
      continue;
    }

    const errorMatch = errorRegex.exec(line);
    if (errorMatch && currentFile) {
      const [, lineNum, column, severity, message, source] = errorMatch;

      // Only include errors and critical warnings
      if (severity === 'error' || severity === 'warning') {
        errors.push({
          type: severity,
          path: currentFile,
          obj: source.split('.').pop(),
          message: message,
          line: parseInt(lineNum, 10),
          column: column ? parseInt(column, 10) : 0
        });
      }
    }
  }

  return errors;
}

/**
 * Ensure ESLint is installed for frontend linting
 */
function ensureEslint() {
  const frontendNodeModules = path.join(FRONTEND_DIR, 'node_modules');
  const eslintPath = path.join(frontendNodeModules, '.bin', 'eslint');

  if (!fs.existsSync(frontendNodeModules)) {
    console.error('[lint] Frontend dependencies not installed');
    return null;
  }

  // Check if ESLint is already installed
  const packageJsonPath = path.join(FRONTEND_DIR, 'package.json');
  if (fs.existsSync(packageJsonPath)) {
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
    const hasEslint = (packageJson.devDependencies && packageJson.devDependencies.eslint) ||
                     (packageJson.dependencies && packageJson.dependencies.eslint);

    if (!hasEslint) {
      console.error('[lint] Installing ESLint...');
      try {
        execSync(
          'npm install --save-dev eslint @eslint/js typescript-eslint',
          { cwd: FRONTEND_DIR, stdio: 'inherit', shell: true }
        );
      } catch (error) {
        console.error('[lint] ERROR: Failed to install ESLint');
        return null;
      }
    }
  }

  return eslintPath;
}

/**
 * Create default ESLint configuration if not exists
 */
function ensureEslintConfig() {
  const configPath = path.join(FRONTEND_DIR, 'eslint.config.mjs');

  if (fs.existsSync(configPath)) {
    return configPath;
  }

  console.error('[lint] Creating default ESLint configuration...');

  const defaultConfig = `import js from '@eslint/js';
import tseslint from 'typescript-eslint';

export default [
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    rules: {
      '@typescript-eslint/no-unused-vars': 'error',
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-console': 'off',
    },
  },
  {
    ignores: ['dist/', 'node_modules/', 'vite.config.ts'],
  },
];
`;

  fs.writeFileSync(configPath, defaultConfig, 'utf8');
  console.error('[lint] ESLint configuration created at', configPath);
  return configPath;
}

/**
 * Lint frontend TypeScript/React code using ESLint
 */
function lintFrontend() {
  console.error('[lint] Linting frontend TypeScript code...');

  if (!fs.existsSync(FRONTEND_DIR)) {
    console.error('[lint] Frontend directory not found, skipping frontend linting');
    return [];
  }

  const srcDir = path.join(FRONTEND_DIR, 'src');
  if (!fs.existsSync(srcDir)) {
    console.error('[lint] Frontend source directory not found, skipping frontend linting');
    return [];
  }

  ensureEslint();
  ensureEslintConfig();

  const outputPath = path.join(ROOT_DIR, 'tools', 'eslint-result.json');

  try {
    const npxCmd = process.platform === 'win32' ? 'npx.cmd' : 'npx';
    execSync(
      `${npxCmd} eslint "${srcDir}" --format json --output-file "${outputPath}"`,
      { cwd: FRONTEND_DIR, stdio: 'ignore', shell: true }
    );
  } catch (error) {
    // ESLint exits with non-zero if there are violations
    console.error('[lint] ESLint found issues');
  }

  // Parse JSON output
  if (!fs.existsSync(outputPath)) {
    return [];
  }

  const jsonContent = fs.readFileSync(outputPath, 'utf8');
  let eslintResults;

  try {
    eslintResults = JSON.parse(jsonContent);
  } catch (error) {
    console.error('[lint] ERROR: Failed to parse ESLint output');
    return [];
  }

  const errors = parseEslintJson(eslintResults);

  // Clean up temporary file
  try {
    fs.unlinkSync(outputPath);
  } catch (error) {
    // Ignore cleanup errors
  }

  console.error(`[lint] Frontend: Found ${errors.length} issue(s)`);
  return errors;
}

/**
 * Parse ESLint JSON output
 */
function parseEslintJson(results) {
  const errors = [];

  for (const file of results) {
    if (!file.messages || file.messages.length === 0) {
      continue;
    }

    for (const msg of file.messages) {
      // Only include errors and warnings
      if (msg.severity === 2 || msg.severity === 1) {
        errors.push({
          type: msg.severity === 2 ? 'error' : 'warning',
          path: file.filePath,
          obj: msg.ruleId || 'unknown',
          message: msg.message,
          line: msg.line || 0,
          column: msg.column || 0
        });
      }
    }
  }

  return errors;
}

/**
 * Main linting routine
 */
function main() {
  console.error('[lint] CBR Viewer - Code Linter');
  console.error('[lint] Platform:', process.platform);

  // Ensure dependencies are installed (silent)
  if (!ensureDependencies()) {
    console.error('[lint] ERROR: Cannot run linting without dependencies');
    // Output empty JSON array to stdout
    console.log(JSON.stringify([]));
    process.exit(1);
  }

  const allErrors = [];

  // Lint backend
  try {
    const backendErrors = lintBackend();
    allErrors.push(...backendErrors);
  } catch (error) {
    console.error('[lint] ERROR during backend linting:', error.message);
  }

  // Lint frontend
  try {
    const frontendErrors = lintFrontend();
    allErrors.push(...frontendErrors);
  } catch (error) {
    console.error('[lint] ERROR during frontend linting:', error.message);
  }

  console.error(`[lint] Total issues found: ${allErrors.length}`);

  // Output JSON to stdout (ONLY valid JSON, nothing else)
  console.log(JSON.stringify(allErrors, null, 2));

  // Exit with appropriate code
  const hasErrors = allErrors.some(err => err.type === 'error');
  if (hasErrors || allErrors.length > 0) {
    process.exit(1);
  } else {
    process.exit(0);
  }
}

// Run if executed directly
if (require.main === module) {
  main();
}

module.exports = {
  lintBackend,
  lintFrontend
};
