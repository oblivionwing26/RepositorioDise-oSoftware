import { Routes } from '@angular/router';
import { Compra } from './compra/compra';

export const routes: Routes = [
    // { path: '', redirectTo: 'comprar', pathMatch: 'full' },
    { path: 'comprar', component: Compra }
];
