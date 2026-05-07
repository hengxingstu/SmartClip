import fs from 'node:fs';
import path from 'node:path';

const desktopRoot = path.resolve(process.cwd());
const repoRoot = path.resolve(desktopRoot, '..');
const appRoot = path.join(desktopRoot, 'app');
const frontendTarget = path.join(appRoot, 'frontend-dist');
const backendTarget = path.join(appRoot, 'backend');
const runtimeTarget = path.join(appRoot, 'runtime', 'win-jre');

const frontendSourceCandidates = [
  process.env.SMARTCLIP_FRONTEND_BUILD,
  path.join(repoRoot, 'frontend', 'dist'),
  path.join(repoRoot, 'src', 'main', 'resources', 'static')
].filter(Boolean);

const backendJarCandidates = [
  process.env.SMARTCLIP_BACKEND_JAR,
  ...findJarCandidates(path.join(repoRoot, 'target'))
].filter(Boolean);

const runtimeSource = process.env.SMARTCLIP_JRE_DIR || '';

fs.mkdirSync(frontendTarget, { recursive: true });
fs.mkdirSync(backendTarget, { recursive: true });
fs.mkdirSync(path.dirname(runtimeTarget), { recursive: true });

const frontendSource = frontendSourceCandidates.find((candidate) =>
  fs.existsSync(path.join(candidate, 'index.html'))
);

if (frontendSource) {
  cleanDirectory(frontendTarget);
  copyDirectory(frontendSource, frontendTarget);
  console.log(`Copied frontend build: ${frontendSource} -> ${frontendTarget}`);
} else {
  console.warn('Frontend build not found. Expected one of:');
  frontendSourceCandidates.forEach((candidate) => console.warn(`  - ${candidate}`));
}

const backendJar = backendJarCandidates.find((candidate) => fs.existsSync(candidate));
if (backendJar) {
  const jarTarget = path.join(backendTarget, 'smartclip-backend.jar');
  removeJarFiles(backendTarget);
  fs.copyFileSync(backendJar, jarTarget);
  console.log(`Copied backend jar: ${backendJar} -> ${jarTarget}`);
} else {
  console.warn('Backend jar not found. Expected target/*.jar or SMARTCLIP_BACKEND_JAR.');
}

if (runtimeSource && fs.existsSync(path.join(runtimeSource, 'bin', 'java.exe'))) {
  cleanDirectory(runtimeTarget);
  copyDirectory(runtimeSource, runtimeTarget);
  console.log(`Copied runtime: ${runtimeSource} -> ${runtimeTarget}`);
} else if (runtimeSource) {
  console.warn(`SMARTCLIP_JRE_DIR is set but java.exe was not found: ${runtimeSource}`);
}

function findJarCandidates(targetDir) {
  if (!fs.existsSync(targetDir)) {
    return [];
  }

  return fs
    .readdirSync(targetDir, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.endsWith('.jar') && !entry.name.endsWith('-sources.jar'))
    .map((entry) => path.join(targetDir, entry.name))
    .sort((left, right) => fs.statSync(right).mtimeMs - fs.statSync(left).mtimeMs);
}

function copyDirectory(source, target) {
  fs.cpSync(source, target, { recursive: true, force: true });
}

function cleanDirectory(target) {
  if (!fs.existsSync(target)) {
    return;
  }

  for (const entry of fs.readdirSync(target)) {
    if (entry === '.gitkeep') {
      continue;
    }
    try {
      fs.rmSync(path.join(target, entry), { recursive: true, force: true });
    } catch (error) {
      console.warn(`Skipped cleaning locked entry: ${path.join(target, entry)} (${error.message})`);
    }
  }
}

function removeJarFiles(target) {
  if (!fs.existsSync(target)) {
    return;
  }

  for (const entry of fs.readdirSync(target)) {
    if (entry.toLowerCase().endsWith('.jar')) {
      try {
        fs.rmSync(path.join(target, entry), { force: true });
      } catch (error) {
        console.warn(`Skipped cleaning locked jar: ${path.join(target, entry)} (${error.message})`);
      }
    }
  }
}
