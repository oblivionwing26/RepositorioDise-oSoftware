import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: '', renderMode: RenderMode.Server },
  { path: 'comprar', renderMode: RenderMode.Server },
  { path: '**', renderMode: RenderMode.Server }
];
