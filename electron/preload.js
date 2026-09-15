const { contextBridge } = require('electron');

// IPC bridge is intentionally minimal: SyncSlot currently needs no native
// desktop integration beyond hosting the window, so nothing is exposed.
// If native features are added later, expose only narrow, explicit APIs here.
contextBridge.exposeInMainWorld('syncslot', {
  platform: process.platform,
});
