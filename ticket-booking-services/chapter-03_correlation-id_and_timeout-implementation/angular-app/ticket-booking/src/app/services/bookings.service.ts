import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API } from '../core/api.config';
import { Observable } from 'rxjs';

export interface Booking {
  bookingId: string;
  userId: string;
  showId: string;
  qty: number;
  status: string;
  holdId?: string;
  createdAt?: string;

  // if your backend returns these too (your BookingController does)
  paymentId?: string;
  amount?: number;
  paymentStatus?: string;
}

@Injectable({ providedIn: 'root' })
export class BookingsService {
  constructor(private http: HttpClient) {}

  create(
    userId: string,
    showId: string,
    qty: number,
    amount: number,
    failPayment: boolean = false
  ): Observable<Booking> {
    return this.http.post<Booking>(
      `${API.booking}?userId=${encodeURIComponent(userId)}&showId=${encodeURIComponent(showId)}&qty=${qty}&amount=${amount}&failPayment=${failPayment}`,
      {}
    );
  }

  get(bookingId: string): Observable<Booking> {
    return this.http.get<Booking>(`${API.booking}/${encodeURIComponent(bookingId)}`);
  }

  confirm(bookingId: string): Observable<any> {
    return this.http.post(`${API.booking}/${encodeURIComponent(bookingId)}/confirm`, {});
  }

  cancel(bookingId: string): Observable<any> {
    return this.http.post(`${API.booking}/${encodeURIComponent(bookingId)}/cancel`, {});
  }
}
