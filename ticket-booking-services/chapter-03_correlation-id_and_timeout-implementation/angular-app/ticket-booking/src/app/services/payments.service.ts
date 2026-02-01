import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API } from '../core/api.config';
import { Observable } from 'rxjs';

export interface Payment {
  paymentId: string;
  bookingId: string;
  status: string;
  amount: number;
}

@Injectable({ providedIn: 'root' })
export class PaymentsService {
  constructor(private http: HttpClient) {}

  pay(bookingId: string, amount: number, fail: boolean): Observable<Payment> {
    return this.http.post<Payment>(
      `${API.payment}/pay?bookingId=${encodeURIComponent(bookingId)}&amount=${amount}&fail=${fail}`,
      {}
    );
  }

  status(bookingId: string): Observable<Payment> {
    return this.http.get<Payment>(`${API.payment}/status/${encodeURIComponent(bookingId)}`);
  }

  refund(bookingId: string): Observable<any> {
    return this.http.post(`${API.payment}/refund/${encodeURIComponent(bookingId)}`, {});
  }
}
