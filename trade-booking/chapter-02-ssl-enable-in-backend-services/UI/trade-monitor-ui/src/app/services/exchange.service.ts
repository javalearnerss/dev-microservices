import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { Observable } from 'rxjs';
import { API } from '../core/api.config';

@Injectable({
  providedIn: 'root'
})
export class ExchangeService {

  constructor(private http: HttpClient) { }

  getExchangeFeedStatus() : Observable<string>{
    return this.http.get(environment.url+'/exchange/feed/status', {
      responseType: 'text'
    });
  }

  getFeedDetails() :  Observable<Record<string, number>>{
    return this.http.get<Record<string, number>>(`${API.exchangeUrl}/feed/details`);
  }

  startFeed(interval: number) :  Observable<string>{
    return this.http.post(`${API.exchangeUrl}/feed/on?interval=${interval}`, {}, { responseType: 'text' });
  }

  stopFeed() :  Observable<string>{
    return this.http.post(`${API.exchangeUrl}/feed/off`, {}, { responseType: 'text' });
  }
}
