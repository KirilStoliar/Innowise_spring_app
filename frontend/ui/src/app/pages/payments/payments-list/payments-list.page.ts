import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { PaymentService } from '../../../services/payment.service';
import { AuthService } from '../../../services/auth.service';
import { UserService } from '../../../services/user.service';

import { PaymentResponse, PaymentStatus, PaymentSearchParams } from '../../../types/payment.types';

@Component({
  selector: 'app-payments-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payments-list.page.html',
  styleUrls: ['./payments-list.page.css'],
})
export class PaymentsListPage implements OnInit {
  readonly auth = inject(AuthService);
  readonly paymentService = inject(PaymentService);
  private userService = inject(UserService);
  private router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly payments = signal<PaymentResponse[]>([]);

  selectedStatus: '' | PaymentStatus = '';

  ngOnInit() {
    this.loadPayments();
  }

  loadPayments() {
    this.loading.set(true);
    this.error.set(null);

    const params: PaymentSearchParams = {};

    if (!this.auth.isAdmin()) {
      const userId = this.userService.userId();
      if (!userId) {
        this.error.set('User not found. Please login again.');
        this.loading.set(false);
        return;
      }
      params.userId = userId;
    }

    if (this.selectedStatus) {
      params.status = this.selectedStatus;
    }

    this.paymentService.searchByCriteria(params).subscribe({
      next: (payments) => {
        this.payments.set(payments);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load payments');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  applyFilters() {
    this.loadPayments();
  }

  resetFilters() {
    this.selectedStatus = '';
    this.loadPayments();
  }

  navigateTo(path: string) {
    this.router.navigate([path]);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }

  getStatusClass(status: PaymentStatus): string {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'COMPLETED':
        return 'bg-green-100 text-green-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}