import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TradeEnricherStats } from '../models/trade.enricher.stats.model';
import { Observable } from 'rxjs';
import { API } from '../core/api.config';

@Injectable({
  providedIn: 'root'
})
export class TradeEnricherService {

  constructor(private http: HttpClient) { }

  fetchEnricherStats() : Observable<TradeEnricherStats>{
    return this.http.get<TradeEnricherStats>(`${API.tradeEnricherUrl}/metrics`);
  }

}
