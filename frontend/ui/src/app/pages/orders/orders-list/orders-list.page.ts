import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../services/order.service';
import { AuthService } from '../../../services/auth.service';
import { Order, OrderStatus } from '../../../types/order.types';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './orders-list.page.html',
  styleUrl: './orders-list.page.css',
})
export class OrdersListPage implements OnInit {
  readonly auth = inject(AuthService);
  readonly userService = inject(UserService);
  readonly orderService = inject(OrderService);
  private router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly orders = signal<Order[]>([]);
  readonly currentPage = signal(0);
  readonly currentRoute = signal('/orders');

  selectedStatus = '';

  ngOnInit() {
    this.currentRoute.set(this.router.url);
    this.loadUserId();
  }

  loadUserId() {
    this.userService.getUserIdByEmail(this.auth.email() ?? '').subscribe({
      next: (res) => {
        this.userService.setUserId(res.data);
        this.loadOrders();
      },
      error: () => {
        console.error('Failed to fetch user id by email address');
      },
    });
  }

  loadOrders() {
    this.loading.set(true);
    this.error.set(null);

    const params = {
      page: this.currentPage(),
      size: 10,
      ...(this.selectedStatus && { statuses: [this.selectedStatus as OrderStatus] }),
    };

    const request$ = this.auth.isAdmin()
      ? this.orderService.getAllWithFilters(params)
      : this.orderService.getAllOrdersForUsers(params);

    request$.subscribe({
      next: (res) => {
        this.orders.set(res.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load orders');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  applyFilters() {
    this.currentPage.set(0);
    this.loadOrders();
  }

  resetFilters() {
    this.selectedStatus = '';
    this.currentPage.set(0);
    this.loadOrders();
  }

  changePage(page: number) {
    this.currentPage.set(page);
    this.loadOrders();
  }

  canPay(order: Order): boolean {
    // Пользователь может оплатить только свои заказы со статусом PENDING
    // Админ может оплачивать любые заказы
    if (order.status !== 'PENDING') return false;
    if (this.auth.isAdmin()) return true;
    return order.userEmail === this.auth.email();
  }

  canEdit(order: Order): boolean {
    // Админ может редактировать любые заказы
    if (this.auth.isAdmin()) return true;
    // Пользователь может редактировать только свои заказы со статусом PENDING
    return order.status === 'PENDING' && order.userEmail === this.auth.email();
  }

  canDelete(order: Order): boolean {
    // Админ может удалять любые заказы
    if (this.auth.isAdmin()) return true;
    // Пользователь может удалять только свои заказы со статусом PENDING
    return order.status === 'PENDING' && order.userEmail === this.auth.email();
  }

  payOrder(id: number) {
    this.router.navigate(['/orders', id, 'pay']);
  }

  deleteOrder(id: number) {
    if (!confirm('Are you sure you want to delete this order?')) return;

    this.orderService.delete(id).subscribe({
      next: () => this.loadOrders(),
      error: (err) => {
        this.error.set('Failed to delete order');
        console.error(err);
      },
    });
  }

  navigateTo(path: string) {
    this.router.navigate([path]);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }

  getStatusClass(status: OrderStatus): string {
    switch (status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMED':
        return 'bg-green-100 text-green-800';
      case 'SHIPPED':
        return 'bg-blue-100 text-blue-800';
      case 'DELIVERED':
        return 'bg-purple-100 text-purple-800';
      case 'CANCELLED':
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
