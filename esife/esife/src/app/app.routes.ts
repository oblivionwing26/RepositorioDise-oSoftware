import { Routes } from '@angular/router';
import { Compra } from './compra/compra';
import { Espectaculos } from './espectaculos/espectaculos';
import { Login } from './login/login';
import { Register } from './register/register';
import { ForgotPassword } from './forgot-password/forgot-password';
import { ResetPassword } from './reset-password/reset-password';
import { MiCuenta } from './mi-cuenta/mi-cuenta';

export const routes: Routes = [
    { path: '', component: Espectaculos },
    { path: 'comprar', component: Compra },
    { path: 'login', component: Login },
    { path: 'register', component: Register },
    { path: 'forgot-password', component: ForgotPassword },
    { path: 'reset-password', component: ResetPassword },
    { path: 'mi-cuenta', component: MiCuenta }
];
