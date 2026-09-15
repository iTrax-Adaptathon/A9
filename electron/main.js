const { app, BrowserWindow } = require('electron');
const path = require('path');
const fs = require('fs');
const { spawn } = require('child_process');

// Development mode loads the React dev server; a packaged build loads the
// built frontend (frontend/dist) and optionally spawns the Spring Boot JAR.
const isDev = !app.isPackaged;
const DEV_URL = process.env.VITE_DEV_SERVER_URL || 'http://localhost:5173';

let backendProcess = null;

// In a packaged build, launch the backend JAR if present. Java must be on PATH
// (bundling a JRE is deferred to P2 packaging polish).
function startBackend() {
  if (app.isPackaged) {
    const jar = path.join(process.resourcesPath, 'backend', 'syncslot-backend.jar');
    if (fs.existsSync(jar)) {
      backendProcess = spawn('java', ['-jar', jar], { stdio: 'ignore' });
    }
  }
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 860,
    title: 'SyncSlot',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });

  if (isDev) {
    win.loadURL(DEV_URL);
  } else {
    win.loadFile(path.join(__dirname, '..', 'dist', 'index.html'));
  }
}

app.whenReady().then(() => {
  startBackend();
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('will-quit', () => {
  if (backendProcess) backendProcess.kill();
});
