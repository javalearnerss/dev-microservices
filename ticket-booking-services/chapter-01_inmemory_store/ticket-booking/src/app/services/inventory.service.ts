import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API } from '../core/api.config';
import { Observable } from 'rxjs';

export interface ShowInfo {
  showId: string;
  remaining: number;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  constructor(private http: HttpClient) {}

  // ✅ First-time load
  getAllShows(): Observable<ShowInfo[]> {
    // GET /inventory/shows
    return this.http.get<ShowInfo[]>(`${API.inventory}/shows`);
  }

  createOrUpdateShow(showId: string, totalTickets: number): Observable<any> {
    // POST /inventory/shows/{showId}?totalTickets=10
    return this.http.post(
      `${API.inventory}/shows/${encodeURIComponent(showId)}?totalTickets=${totalTickets}`,
      {}
    );
  }

  getShow(showId: string): Observable<any> {
    // GET /inventory/shows/{showId}
    return this.http.get(`${API.inventory}/shows/${encodeURIComponent(showId)}`);
  }
}
