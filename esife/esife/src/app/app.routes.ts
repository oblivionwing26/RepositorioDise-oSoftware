import { Routes } from '@angular/router';
import { Compra } from './compra/compra';
import { Espectaculos } from './espectaculos/espectaculos';
import { Login } from './auth/login/login';
import { Register } from './auth/register/register';

export const routes: Routes = [
  { path: '', component: Espectaculos },
  { path: 'comprar', component: Compra },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
];
