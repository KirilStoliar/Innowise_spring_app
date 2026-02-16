import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  OrderItem,
  PageRequest,
  PageResponse,
  CreateOrderItemRequest,
  UpdateOrderItemRequest,
} from '../types/order.types';

@Injectable({ providedIn: 'root' })
export class OrderItemService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiGatewayUrl}/api/v1/order-items`;

  readonly orderItems = signal<OrderItem[]>([]);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  getAll(params?: PageRequest): Observable<PageResponse<OrderItem>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params?.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<PageResponse<OrderItem>>(this.apiUrl, { params: httpParams }).pipe(
      tap(res => {
        this.orderItems.set(res.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
      })
    );
  }

  getById(id: number): Observable<OrderItem> {
    return this.http.get<OrderItem>(`${this.apiUrl}/${id}`);
  }

  // Получить все OrderItems по ID заказа
  getByOrderId(orderId: number): Observable<OrderItem[]> {
    return this.http.get<OrderItem[]>(`${this.apiUrl}/order/${orderId}`).pipe(
      tap(items => this.orderItems.set(items))
    );
  }

  // Получить все OrderItems по ID пользователя
  getByUserId(userId: number): Observable<OrderItem[]> {
    return this.http.get<OrderItem[]>(`${this.apiUrl}/user/${userId}`).pipe(
      tap(items => this.orderItems.set(items))
    );
  }

  create(req: CreateOrderItemRequest): Observable<OrderItem> {
    return this.http.post<OrderItem>(this.apiUrl, req);
  }

  update(id: number, req: UpdateOrderItemRequest): Observable<OrderItem> {
    return this.http.put<OrderItem>(`${this.apiUrl}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}