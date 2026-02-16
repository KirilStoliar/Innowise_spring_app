import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { OrderService } from '../../../services/order.service';
import { PaymentService } from '../../../services/payment.service';
import { PaymentCardService } from '../../../services/payment-card.service';
import { UserService } from '../../../services/user.service';

import { Order } from '../../../types/order.types';
import { PaymentCard, PaymentCardCreateRequest } from '../../../types/payment-card.types';
import { CreatePaymentRequest, PaymentResponse } from '../../../types/payment.types';

@Component({
  selector: 'app-order-pay',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './order-pay.page.html',
  styleUrls: ['./order-pay.page.css'],
})
export class OrderPayPage implements OnInit, OnDestroy {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  private paymentService = inject(PaymentService);
  private cardService = inject(PaymentCardService);
  private userService = inject(UserService);

  readonly loading = signal(false);
  readonly processing = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly order = signal<Order | null>(null);
  readonly paymentCards = signal<PaymentCard[]>([]);

  private readonly destroy$ = new Subject<void>();

  selectedCardId: number | null = null;

  // add card state
  readonly addCardOpen = signal(false);
  readonly addCardLoading = signal(false);
  readonly addCardError = signal<string | null>(null);

  addCardForm: PaymentCardCreateRequest = {
    number: '',
    holder: '',
    expirationDate: '', // yyyy-MM-dd
  };

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadOrder(+id);
      this.loadPaymentCards();
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadOrder(id: number) {
    this.loading.set(true);
    this.orderService.getById(id).subscribe({
      next: (order) => {
        if (order.status !== 'PENDING') {
          this.error.set('This order cannot be paid (status: ' + order.status + ')');
        }
        this.order.set(order);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load order');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  loadPaymentCards(preselectId?: number) {
    const userId = this.userService.userId();
    if (!userId) return;

    this.cardService.getCardsByUserId(userId).subscribe({
      next: (cards) => {
        const activeCards = cards.filter((c) => c.active);
        this.paymentCards.set(activeCards);

        if (preselectId && activeCards.some((c) => c.id === preselectId)) {
          this.selectedCardId = preselectId;
        } else {
          this.selectedCardId = activeCards[0]?.id ?? null;
        }
      },
      error: (err) => console.error('Failed to load payment cards', err),
    });
  }

  openAddCard() {
    this.addCardError.set(null);
    this.addCardOpen.set(true);
  }

  closeAddCard() {
    this.addCardOpen.set(false);
    this.addCardError.set(null);
    this.addCardLoading.set(false);
    this.addCardForm = { number: '', holder: '', expirationDate: '' };
  }

  onCardNumberInput() {
    this.addCardForm.number = (this.addCardForm.number ?? '').replace(/\D/g, '');
  }

  private validateAddCardForm(): string | null {
    const number = (this.addCardForm.number ?? '').trim();
    const holder = (this.addCardForm.holder ?? '').trim();
    const exp = (this.addCardForm.expirationDate ?? '').trim();

    if (!number) return 'Card number is required';
    if (number.length < 16 || number.length > 19) return 'Card number must be between 16 and 19 digits';
    if (!holder) return 'Card holder is required';
    if (!exp) return 'Expiration date is required';

    const expDate = new Date(exp + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (isNaN(expDate.getTime())) return 'Invalid expiration date';
    if (expDate <= today) return 'Expiration date must be in the future';

    return null;
  }

  createCard() {
    const userId = this.userService.userId();
    if (!userId) return;

    const validationError = this.validateAddCardForm();
    if (validationError) {
      this.addCardError.set(validationError);
      return;
    }

    this.addCardLoading.set(true);
    this.addCardError.set(null);

    this.cardService
      .create(userId, {
        number: this.addCardForm.number.trim(),
        holder: this.addCardForm.holder.trim(),
        expirationDate: this.addCardForm.expirationDate.trim(),
      })
      .subscribe({
        next: (created) => {
          this.addCardLoading.set(false);
          this.closeAddCard();
          this.loadPaymentCards(created.id);
        },
        error: (err) => {
          this.addCardLoading.set(false);
          this.addCardError.set('Failed to create payment card');
          console.error(err);
        },
      });
  }

  pay() {
    const order = this.order();
    if (!order) return;

    // cardId сейчас в API платежа не используется, но блокирует оплату на UI
    if (!this.selectedCardId) return;

    if (order.status !== 'PENDING') {
      this.error.set(`This order cannot be paid (status: ${order.status})`);
      return;
    }

    this.processing.set(true);
    this.error.set(null);
    this.success.set(null);

    const req: CreatePaymentRequest = {
      orderId: order.id,
      userId: order.userId,
      paymentAmount: order.totalPrice,
    };

    this.paymentService
      .createPayment(req) // Observable<PaymentResponse>
      .pipe(finalize(() => this.processing.set(false)))
      .subscribe({
        next: (payment: PaymentResponse) => {
          if (payment.status === 'FAILED') {
            this.error.set('Payment failed. Please try again.');
            return;
          }

          this.success.set('Payment successful!');
          // при желании можно обновить заказ один раз, но без ожиданий/поллинга:
          // this.loadOrder(order.id);

          this.router.navigate(['/orders']);
        },
        error: (err) => {
          this.error.set('Payment failed.');
          console.error(err);
        },
      });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'PAID':
        return 'bg-green-100 text-green-800';
      case 'CANCELLED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }
}