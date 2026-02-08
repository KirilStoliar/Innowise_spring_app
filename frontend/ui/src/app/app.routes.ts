import { Routes } from '@angular/router';
import { authCanMatch } from './guards/auth-guard';
import { adminCanMatch } from './guards/admin-guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'orders' },

  {
    path: 'auth',
    loadComponent: () => import('./pages/auth/auth.page').then((m) => m.Auth),
  },

  {
    path: 'orders',
    canMatch: [authCanMatch],
    loadComponent: () =>
      import('./pages/orders/orders-list/orders-list.page').then((m) => m.OrdersListPage),
  },

  {
    path: 'orders/new',
    canMatch: [authCanMatch],
    loadComponent: () =>
      import('./pages/orders/order-form/order-form.page').then((m) => m.OrderFormPage),
  },

  {
    path: 'orders/:id/edit',
    canMatch: [authCanMatch],
    loadComponent: () =>
      import('./pages/orders/order-form/order-form.page').then((m) => m.OrderFormPage),
  },

  {
    path: 'orders/:id/pay',
    canMatch: [authCanMatch],
    loadComponent: () =>
      import('./pages/orders/order-pay/order-pay.page').then((m) => m.OrderPayPage),
  },

  {
    path: 'payments',
    canMatch: [authCanMatch],
    loadComponent: () =>
      import('./pages/payments/payments-list/payments-list.page').then((m) => m.PaymentsListPage),
  },

  {
    path: 'users',
    canMatch: [adminCanMatch],
    loadComponent: () =>
      import('./pages/users/users-list/users-list.page').then((m) => m.UsersListPage),
  },

  {
    path: 'users/new',
    canMatch: [adminCanMatch],
    loadComponent: () =>
      import('./pages/users/user-form/user-form.page').then((m) => m.UserFormPage),
  },

  { path: '**', redirectTo: 'orders' },
];
