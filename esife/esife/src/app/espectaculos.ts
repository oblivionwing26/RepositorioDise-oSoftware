import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EscenarioDto {
  id: number;
  nombre: string;
  descripcion: string;
  espectaculos?: EspectaculoDto[];
  loading?: boolean;
}

export interface EspectaculoDto {
  id: number;
  artista: string;
  fecha: string;
  escenario: string;
  entradas?: EntradasResumen;
  entradasDisponibles?: EntradaDisponible[];
  loadingEntradas?: boolean;
}

export interface EntradasResumen {
  total: number;
  libres: number;
  reservadas: number;
  vendidas: number;
}

export interface EntradaDisponible {
  id: number;
  precio: number;
  estado: string;
  tipo: 'ZONA' | 'PRECISA' | 'GENERAL';
  zona?: number;
  fila?: number;
  columna?: number;
  planta?: number;
}

@Injectable({
  providedIn: 'root',
})
export class EspectaculosService {
  private readonly BUSQUEDA_API = 'http://localhost:8080/busqueda';

  constructor(private http: HttpClient) {}

  getNumeroDeEntradas(idEspectaculo: number): Observable<number> {
    return this.http.get<number>(`${this.BUSQUEDA_API}/getNumeroDeEntradas/${idEspectaculo}`);
  }

  getNumeroDeEntradasComoDto(idEspectaculo: number): Observable<EntradasResumen> {
    return this.http.get<EntradasResumen>(`${this.BUSQUEDA_API}/getNumeroDeEntradasComoDto/${idEspectaculo}`);
  }

  getEntradasLibres(idEspectaculo: number): Observable<number> {
    return this.http.get<number>(`${this.BUSQUEDA_API}/getEntradasLibres/${idEspectaculo}`);
  }

  getEntradasDisponibles(idEspectaculo: number): Observable<EntradaDisponible[]> {
    return this.http.get<EntradaDisponible[]>(`${this.BUSQUEDA_API}/getEntradasDisponibles/${idEspectaculo}`);
  }

  getEspectaculos(idEscenario: number): Observable<EspectaculoDto[]> {
    return this.http.get<EspectaculoDto[]>(`${this.BUSQUEDA_API}/getEspectaculos/${idEscenario}`);
  }

  getEscenarios(): Observable<EscenarioDto[]> {
    return this.http.get<EscenarioDto[]>(`${this.BUSQUEDA_API}/getEscenarios`);
  }

  buscarEspectaculos(artista: string): Observable<EspectaculoDto[]> {
    return this.http.get<EspectaculoDto[]>(
      `${this.BUSQUEDA_API}/getEspectaculos`,
      {
        params: { artista }
      }
    );
  }
}
