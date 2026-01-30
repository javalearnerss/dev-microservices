import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BookingsService, Booking } from '../../services/bookings.service';

@Component({
  selector: 'app-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bookings.component.html'
})
export class BookingsComponent {
  // Create booking
  userId = 'U-1001';
  showId = 'SHOW-101';
  qty = 2;
  amount = 500;
  failPayment = false;

  // Manage booking
  bookingId = '';

  booking?: Booking;
  err = '';

  loadingCreate = false;
  loadingGet = false;
  loadingConfirm = false;
  loadingCancel = false;

  constructor(private api: BookingsService) {}

  create() {
    this.err = '';
    this.booking = undefined;
    this.loadingCreate = true;

    this.api.create(this.userId, this.showId, this.qty, this.amount, this.failPayment).subscribe({
      next: (res) => {
        this.booking = res;
        this.bookingId = res.bookingId;
        this.loadingCreate = false;
      },
      error: (e) => {
        this.err = e?.error?.error || e?.message || 'Request failed';
        this.loadingCreate = false;
      }
    });
  }

  get() {
    this.err = '';
    this.loadingGet = true;

    this.api.get(this.bookingId).subscribe({
      next: (res) => {
        this.booking = res;
        this.loadingGet = false;
      },
      error: (e) => {
        this.err = e?.error?.error || e?.message || 'Request failed';
        this.loadingGet = false;
      }
    });
  }

  confirm() {
    this.err = '';
    this.loadingConfirm = true;

    this.api.confirm(this.bookingId).subscribe({
      next: () => {
        this.loadingConfirm = false;
        this.get();
      },
      error: (e) => {
        this.err = e?.error?.error || e?.message || 'Request failed';
        this.loadingConfirm = false;
      }
    });
  }

  cancel() {
    this.err = '';
    this.loadingCancel = true;

    this.api.cancel(this.bookingId).subscribe({
      next: () => {
        this.loadingCancel = false;
        this.get();
      },
      error: (e) => {
        this.err = e?.error?.error || e?.message || 'Request failed';
        this.loadingCancel = false;
      }
    });
  }
}
