const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('smartclipDesktop', {
  isDesktop: true,
  backendBaseUrl: `http://${process.env.SMARTCLIP_BACKEND_HOST || '127.0.0.1'}:${process.env.SMARTCLIP_BACKEND_PORT || '8080'}`
});
