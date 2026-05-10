import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ClienteAnonimoService {
  private readonly STORAGE_KEY = 'clienteAnonimoId';

  getId(): string {
    if (typeof sessionStorage === 'undefined') {
      return this.generarId();
    }

    const existente = sessionStorage.getItem(this.STORAGE_KEY);
    if (existente) {
      return existente;
    }

    const nuevo = this.generarId();
    sessionStorage.setItem(this.STORAGE_KEY, nuevo);
    return nuevo;
  }

  private generarId(): string {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return `anon-${crypto.randomUUID()}`;
    }

    return `anon-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
