import fs from 'node:fs';
import path from 'node:path';

const desktopRoot = process.cwd();
const requiredFiles = [
  {
    label: 'Frontend entry',
    file: path.join(desktopRoot, 'app', 'frontend-dist', 'index.html'),
    help: 'Place the built frontend into desktop/app/frontend-dist/.'
  },
  {
    label: 'Java runtime',
    file: path.join(desktopRoot, 'app', 'runtime', 'win-jre', 'bin', 'java.exe'),
    help: 'Place the jlink runtime into desktop/app/runtime/win-jre/.'
  }
];

const backendDir = path.join(desktopRoot, 'app', 'backend');
const jarFiles = fs.existsSync(backendDir)
  ? fs.readdirSync(backendDir).filter((name) => name.toLowerCase().endsWith('.jar'))
  : [];

const missing = requiredFiles.filter((item) => !fs.existsSync(item.file));

if (jarFiles.length === 0) {
  missing.push({
    label: 'Backend jar',
    file: backendDir,
    help: 'Place the Spring Boot jar into desktop/app/backend/.'
  });
}

if (missing.length > 0) {
  console.error('Desktop packaging prerequisites are incomplete:\n');
  for (const item of missing) {
    console.error(`- ${item.label}: ${item.file}`);
    console.error(`  ${item.help}`);
  }
  process.exit(1);
}

console.log('Desktop package layout looks good.');
