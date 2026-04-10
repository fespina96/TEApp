import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.teapp.miagenda',
  appName: 'TEApp – Mi Agenda',
  webDir: 'dist/teapp-frontend/browser',
  server: {
    // En desarrollo con emulador AVD, el backend corre en el host (10.0.2.2).
    // Descomentar la línea siguiente para hot-reload desde el emulador:
    // url: 'http://10.0.2.2:4200',
    // cleartext: true
  },
  android: {
    allowMixedContent: true
  }
};

export default config;
