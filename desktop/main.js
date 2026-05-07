const { app, BrowserWindow, Menu, Tray, nativeImage, dialog } = require('electron');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');

const APP_NAME = 'SmartClip';
const DEFAULT_BACKEND_PORT = Number(process.env.SMARTCLIP_BACKEND_PORT || '8080');
const DEFAULT_BACKEND_HOST = process.env.SMARTCLIP_BACKEND_HOST || '127.0.0.1';
const BACKEND_START_TIMEOUT_MS = 30000;

let mainWindow = null;
let tray = null;
let backendProcess = null;
let isQuitting = false;
let trayHintShown = false;

if (!app.requestSingleInstanceLock()) {
  app.quit();
}

app.on('second-instance', () => {
  showMainWindow();
});

app.whenReady().then(async () => {
  app.setAppUserModelId('com.smartclip.desktop');

  createTray();
  createMainWindow();

  try {
    await startBackend();
    await waitForBackend();
    await loadRenderer();
  } catch (error) {
    await showStartupError(error);
  }
});

app.on('activate', () => {
  showMainWindow();
});

app.on('window-all-closed', () => {
  // Keep the app alive in the tray unless the explicit Exit action is used.
});

app.on('before-quit', () => {
  isQuitting = true;
});

app.on('will-quit', () => {
  stopBackend();
});

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1320,
    height: 860,
    minWidth: 1080,
    minHeight: 720,
    show: false,
    autoHideMenuBar: true,
    backgroundColor: '#101418',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false
    }
  });

  mainWindow.on('close', (event) => {
    if (isQuitting) {
      return;
    }

    event.preventDefault();
    mainWindow.hide();

    if (!trayHintShown) {
      trayHintShown = true;
      if (process.platform === 'win32') {
        tray?.displayBalloon({
          iconType: 'info',
          title: APP_NAME,
          content: 'Window hidden to tray. Use the tray menu to exit.'
        });
      }
    }
  });

  mainWindow.on('ready-to-show', () => {
    mainWindow.show();
  });

  mainWindow.loadURL(buildLoadingPage());
}

function createTray() {
  const trayIcon = createTrayIcon();
  tray = new Tray(trayIcon);
  tray.setToolTip(APP_NAME);
  tray.on('double-click', () => {
    showMainWindow();
  });
  tray.on('click', () => {
    showMainWindow();
  });
  rebuildTrayMenu();
}

function rebuildTrayMenu() {
  const menu = Menu.buildFromTemplate([
    {
      label: 'Open SmartClip',
      click: () => showMainWindow()
    },
    {
      label: 'Restart Backend',
      click: async () => {
        try {
          await restartBackend();
        } catch (error) {
          await dialog.showMessageBox({
            type: 'error',
            title: 'Restart Failed',
            message: error.message
          });
        }
      }
    },
    { type: 'separator' },
    {
      label: 'Exit',
      click: () => {
        isQuitting = true;
        app.quit();
      }
    }
  ]);

  tray.setContextMenu(menu);
}

function showMainWindow() {
  if (!mainWindow) {
    return;
  }

  if (mainWindow.isMinimized()) {
    mainWindow.restore();
  }

  mainWindow.show();
  mainWindow.focus();
}

async function loadRenderer() {
  if (!mainWindow) {
    return;
  }

  const rendererUrl = process.env.ELECTRON_RENDERER_URL;
  if (rendererUrl) {
    await mainWindow.loadURL(rendererUrl);
    return;
  }

  const frontendEntry = getFrontendEntry();
  if (!fs.existsSync(frontendEntry)) {
    throw new Error(
      `Frontend entry file was not found: ${frontendEntry}\n` +
        'Place the built frontend into desktop/app/frontend-dist/.'
    );
  }

  await mainWindow.loadFile(frontendEntry);
}

async function restartBackend() {
  stopBackend();
  await startBackend();
  await waitForBackend();
}

async function startBackend() {
  const javaCommand = getJavaExecutable();
  const jarFile = getBackendJarPath();
  const userDataDir = app.getPath('userData');
  const dataDir = path.join(userDataDir, 'data');
  const dbPath = path.join(dataDir, 'smartclip.db');

  fs.mkdirSync(dataDir, { recursive: true });

  if (!fs.existsSync(javaCommand)) {
    throw new Error(
      `Java runtime was not found: ${javaCommand}\n` +
        'Place the slim JRE into desktop/app/runtime/win-jre/.'
    );
  }

  if (!fs.existsSync(jarFile)) {
    throw new Error(
      `Backend jar was not found: ${jarFile}\n` + 'Place the Spring Boot jar into desktop/app/backend/.'
    );
  }

  const args = [
    '-Djava.awt.headless=false',
    '-Dspring.main.headless=false',
    '-jar',
    jarFile,
    `--server.port=${DEFAULT_BACKEND_PORT}`,
    `--spring.datasource.url=jdbc:sqlite:${toJavaPath(dbPath)}`,
    `--smartclip.data-dir=${toJavaPath(dataDir)}`
  ];

  await new Promise((resolve, reject) => {
    let settled = false;

    backendProcess = spawn(javaCommand, args, {
      cwd: getBackendWorkDir(),
      stdio: 'inherit',
      windowsHide: true
    });

    backendProcess.once('spawn', () => {
      settled = true;
      resolve();
    });

    backendProcess.once('error', (error) => {
      if (!settled) {
        settled = true;
        reject(new Error(`Failed to start backend: ${error.message}`));
      }
    });

    backendProcess.once('exit', (code, signal) => {
      const exitedDuringQuit = isQuitting;
      backendProcess = null;

      if (!settled) {
        settled = true;
        reject(new Error(`Backend exited immediately. exitCode=${code ?? 'null'}, signal=${signal ?? 'null'}`));
        return;
      }

      if (!exitedDuringQuit && code !== 0) {
        dialog.showErrorBox(
          'Backend Stopped',
          `The Java backend exited unexpectedly.\nexitCode=${code ?? 'null'}, signal=${signal ?? 'null'}`
        );
      }
    });
  });
}

function stopBackend() {
  if (!backendProcess || backendProcess.killed) {
    return;
  }

  try {
    backendProcess.kill();
  } catch (error) {
    console.error('Failed to stop backend process:', error);
  }
}

async function waitForBackend() {
  const startedAt = Date.now();

  while (Date.now() - startedAt < BACKEND_START_TIMEOUT_MS) {
    if (!backendProcess) {
      throw new Error('Backend process is no longer running.');
    }

    const ready = await isBackendReady();
    if (ready) {
      return;
    }

    await sleep(700);
  }

  throw new Error(
    `Backend did not become ready within ${BACKEND_START_TIMEOUT_MS / 1000} seconds. ` +
      'Check the jar, runtime, and port settings.'
  );
}

function isBackendReady() {
  return new Promise((resolve) => {
    const request = http.get(
      {
        host: DEFAULT_BACKEND_HOST,
        port: DEFAULT_BACKEND_PORT,
        path: '/',
        timeout: 2000
      },
      (response) => {
        response.resume();
        resolve(response.statusCode >= 200 && response.statusCode < 500);
      }
    );

    request.on('timeout', () => {
      request.destroy();
      resolve(false);
    });

    request.on('error', () => {
      resolve(false);
    });
  });
}

function getFrontendEntry() {
  const frontendDir =
    process.env.SMARTCLIP_FRONTEND_DIR ||
    path.join(getAppResourceRoot(), 'app', 'frontend-dist');
  return path.join(frontendDir, 'index.html');
}

function getBackendJarPath() {
  const configuredJar = process.env.SMARTCLIP_BACKEND_JAR;
  if (configuredJar) {
    return configuredJar;
  }

  const backendDir = path.join(getAppResourceRoot(), 'app', 'backend');
  if (!fs.existsSync(backendDir)) {
    return path.join(backendDir, 'smartclip-backend.jar');
  }

  const stableJar = path.join(backendDir, 'smartclip-backend.jar');
  if (fs.existsSync(stableJar)) {
    return stableJar;
  }

  const jarFiles = fs
    .readdirSync(backendDir, { withFileTypes: true })
    .filter((entry) => entry.isFile() && entry.name.toLowerCase().endsWith('.jar'))
    .map((entry) => path.join(backendDir, entry.name))
    .sort((left, right) => fs.statSync(right).mtimeMs - fs.statSync(left).mtimeMs);

  if (jarFiles.length === 0) {
    return path.join(backendDir, 'smartclip-backend.jar');
  }

  return jarFiles[0];
}

function getBackendWorkDir() {
  return process.env.SMARTCLIP_BACKEND_WORKDIR || path.join(getAppResourceRoot(), 'app', 'backend');
}

function getJavaExecutable() {
  const configuredRuntime = process.env.SMARTCLIP_JRE_DIR;
  if (configuredRuntime) {
    return path.join(configuredRuntime, 'bin', process.platform === 'win32' ? 'java.exe' : 'java');
  }

  return path.join(
    getAppResourceRoot(),
    'app',
    'runtime',
    process.platform === 'win32' ? 'win-jre' : 'jre',
    'bin',
    process.platform === 'win32' ? 'java.exe' : 'java'
  );
}

function getAppResourceRoot() {
  return app.isPackaged ? process.resourcesPath : __dirname;
}

function toJavaPath(filePath) {
  return filePath.replace(/\\/g, '/');
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function createTrayIcon() {
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64">
      <rect x="8" y="8" width="48" height="48" rx="12" fill="#1b8f6a"/>
      <rect x="19" y="18" width="26" height="4" rx="2" fill="#ffffff"/>
      <rect x="19" y="29" width="22" height="4" rx="2" fill="#dff7ee"/>
      <rect x="19" y="40" width="16" height="4" rx="2" fill="#dff7ee"/>
    </svg>
  `.trim();

  return nativeImage
    .createFromDataURL(`data:image/svg+xml;base64,${Buffer.from(svg).toString('base64')}`)
    .resize({ width: 16, height: 16 });
}

function buildLoadingPage() {
  const html = `
    <!doctype html>
    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <title>${APP_NAME}</title>
        <style>
          body {
            margin: 0;
            min-height: 100vh;
            display: grid;
            place-items: center;
            background: radial-gradient(circle at top, #1b8f6a 0%, #101418 60%);
            color: #f4f8f7;
            font-family: "Microsoft YaHei", sans-serif;
          }
          .panel {
            padding: 28px 34px;
            border-radius: 18px;
            background: rgba(15, 18, 22, 0.7);
            box-shadow: 0 18px 50px rgba(0, 0, 0, 0.28);
            backdrop-filter: blur(10px);
          }
          h1 {
            margin: 0 0 10px;
            font-size: 22px;
          }
          p {
            margin: 0;
            color: #d0dbd8;
            font-size: 14px;
          }
        </style>
      </head>
      <body>
        <div class="panel">
          <h1>${APP_NAME} is starting</h1>
          <p>Electron shell and Java backend are being initialized...</p>
        </div>
      </body>
    </html>
  `;

  return `data:text/html;charset=UTF-8,${encodeURIComponent(html)}`;
}

async function showStartupError(error) {
  console.error(error);

  await dialog.showMessageBox({
    type: 'error',
    title: 'Startup Failed',
    message: 'The desktop app could not be started.',
    detail: error instanceof Error ? error.message : String(error)
  });

  isQuitting = true;
  app.quit();
}
