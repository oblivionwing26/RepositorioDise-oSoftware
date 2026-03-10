import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root',
})
export class EspectaculosService {
  getNumeroDeEntradas(id: any) {
    return this.http.get(`http://localhost:8080/busqueda/getNumeroDeEntradas/${id}`);
  }

    getNumeroDeEntradasComoDto(id: any) {
    return this.http.get(`http://localhost:8080/busqueda/getNumeroDeEntradasComoDto/${id}`);
  }

  getEntradasLibres(id: any) {
    return this.http.get(`http://localhost:8080/busqueda/getEntradasLibres/${id}`);
  }

    constructor(private http: HttpClient) {}

  
  getEspectaculos(escenario: any) {
    return this.http.get(`http://localhost:8080/busqueda/getEspectaculos/${escenario}`);
  }

  getEscenarios() {
    return this.http.get('http://localhost:8080/busqueda/getEscenarios');
  }

  
}
