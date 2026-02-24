import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { TradeSymbol } from '../models/trade-symbol.model';
import { RefDataResponse } from '../models/ref-data-response.model';
import { Observable } from 'rxjs';
import { RefdataStats } from '../models/refdata.stats.model';
import { API } from '../core/api.config';

@Injectable({
  providedIn: 'root'
})
export class TradeRefDataService {


  constructor(private http: HttpClient) { }

  getIsin(symbols: string): Observable<RefDataResponse> {
    const params = new HttpParams().set('symbols', symbols);
    return this.http.get<RefDataResponse>(`${API.refdataProviderUrl}/symbol`, { params });
  }


  getStats() : Observable<RefdataStats>{
    return this.http.get<RefdataStats>(`${API.refdataProviderUrl}/symbol/stats`);
  }


}

