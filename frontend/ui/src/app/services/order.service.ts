import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  Order,
  PageRequest,
  PageResponse,
  CreateOrderRequest,
  UpdateOrderRequest,
  OrderStatus,
} from '../types/order.types';
import { UserService } from './user.service';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private http = inject(HttpClient);
  private userService = inject(UserService);
  private readonly apiUrl = `${environment.apiGatewayUrl}/api/v1/orders`;

  readonly orders = signal<Order[]>([]);
  readonly currentOrder = signal<Order | null>(null);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  getAll(params?: PageRequest): Observable<PageResponse<Order>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params?.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<PageResponse<Order>>(this.apiUrl, { params: httpParams }).pipe(
      tap((res) => {
        this.orders.set(res.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
      })
    );
  }

  getAllOrdersForUsers(params?: PageRequest) {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params?.sort) httpParams = httpParams.set('sort', params.sort);
    
    return this.http
      .get<PageResponse<Order>>(`${this.apiUrl}/user/${this.userService.userId()}`, {
        params: httpParams,
      })
      .pipe(
        tap((res) => {
          this.orders.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
        })
      );
  }

  // Получить заказы с фильтрами (по статусам)
  getAllWithFilters(params: {
    page?: number;
    size?: number;
    sort?: string;
    statuses?: OrderStatus[];
  }): Observable<PageResponse<Order>> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.sort) httpParams = httpParams.set('sort', params.sort);
    if (params.statuses && params.statuses.length > 0) {
      params.statuses.forEach((status) => {
        httpParams = httpParams.append('statuses', status);
      });
    }

    return this.http.get<PageResponse<Order>>(this.apiUrl, { params: httpParams }).pipe(
      tap((res) => {
        this.orders.set(res.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
      })
    );
  }

  getById(id: number): Observable<Order> {
    return this.http
      .get<Order>(`${this.apiUrl}/${id}`)
      .pipe(tap((order) => this.currentOrder.set(order)));
  }

  // Получить заказы конкретного пользователя (для админа)
  getByUserId(userId: number, params?: PageRequest): Observable<PageResponse<Order>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params?.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http
      .get<PageResponse<Order>>(`${this.apiUrl}/user/${userId}`, { params: httpParams })
      .pipe(
        tap((res) => {
          this.orders.set(res.content);
          this.totalPages.set(res.totalPages);
          this.totalElements.set(res.totalElements);
        })
      );
  }

  create(req: CreateOrderRequest): Observable<Order> {
    return this.http.post<Order>(this.apiUrl, req);
  }

  update(id: number, req: UpdateOrderRequest): Observable<Order> {
    return this.http.put<Order>(`${this.apiUrl}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
