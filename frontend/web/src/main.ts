import 'zone.js';

;(window as any).global = window
;(window as any).process = { env: {} }

import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app/app.routes';
import { ChatComponent } from './app/features/chat/chat';

bootstrapApplication(ChatComponent, {
  providers: [
    provideHttpClient(withFetch()),
    provideRouter(routes),
  ],
}).catch(err => console.error(err));
