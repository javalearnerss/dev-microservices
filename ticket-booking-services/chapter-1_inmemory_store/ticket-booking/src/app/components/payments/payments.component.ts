import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentsService, Payment } from '../../services/payments.service';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payments.component.html'
})
export class PaymentsComponent {

  bookingId = '';
  amount = 500;
  fail = false;

  payment?: Payment;
  err = '';

  constructor(private api: PaymentsService) {}

  /**
   * Pay for a booking.
   */
  pay() {
    this.err = '';
    this.payment = undefined;

    this.api.pay(this.bookingId, this.amount, this.fail).subscribe({
      next: (res) => (this.payment = res),
      error: (e) => (this.err = e?.error?.error || e?.message || 'Request failed')
    });
  }

  /**
   * Get payment status.
   */
  status() {
    this.err = '';
    this.api.status(this.bookingId).subscribe({
      next: (res) => (this.payment = res),
      error: (e) => (this.err = e?.error?.error || e?.message || 'Request failed')
    });
  }

  /**
   * Refund a successful payment.
   */
  refund() {
    this.err = '';
    this.api.refund(this.bookingId).subscribe({
      next: () => this.status(),
      error: (e) => (this.err = e?.error?.error || e?.message || 'Request failed')
    });
  }
}
