import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  CreatePaymentRequest,
  PaymentResponse,
  PaymentSearchParams,
  PaymentStatus,
} from '../types/payment.types';
import { ApiResponse } from '../types/auth.types';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiGatewayUrl || ''}/api/v1/payments`;

  readonly payments = signal<PaymentResponse[]>([]);
  readonly currentPayment = signal<PaymentResponse | null>(null);

  getAll(): Observable<PaymentResponse[]> {
    return this.http
      .get<ApiResponse<PaymentResponse[]>>(`${this.apiUrl}/search`)
      .pipe(
        map((r) => r.data),
        tap((payments) => this.payments.set(payments))
      );
  }

  getByUserId(userId: number): Observable<PaymentResponse[]> {
    return this.http
      .get<ApiResponse<PaymentResponse[]>>(`${this.apiUrl}/user/${userId}`)
      .pipe(
        map((r) => r.data),
        tap((payments) => this.payments.set(payments))
      );
  }

  getByOrderId(orderId: number): Observable<PaymentResponse[]> {
    return this.http
      .get<ApiResponse<PaymentResponse[]>>(`${this.apiUrl}/order/${orderId}`)
      .pipe(
        map((r) => r.data),
        tap((payments) => this.payments.set(payments))
      );
  }

  getByStatus(status: PaymentStatus): Observable<PaymentResponse[]> {
    return this.http
      .get<ApiResponse<PaymentResponse[]>>(`${this.apiUrl}/status/${status}`)
      .pipe(
        map((r) => r.data),
        tap((payments) => this.payments.set(payments))
      );
  }

  searchByCriteria(params: PaymentSearchParams): Observable<PaymentResponse[]> {
    let httpParams = new HttpParams();
    if (params.userId != null) httpParams = httpParams.set('userId', String(params.userId));
    if (params.orderId != null) httpParams = httpParams.set('orderId', String(params.orderId));
    if (params.status) httpParams = httpParams.set('status', params.status);

    return this.http
      .get<ApiResponse<PaymentResponse[]>>(`${this.apiUrl}/search`, { params: httpParams })
      .pipe(
        map((r) => r.data),
        tap((payments) => this.payments.set(payments))
      );
  }

  getTotalByUserId(userId: number, startDate: string, endDate: string): Observable<number> {
    return this.http
      .get<ApiResponse<number>>(`${this.apiUrl}/user/${userId}/total`, {
        params: { startDate, endDate },
      })
      .pipe(map((r) => r.data));
  }

  getTotalAllUsers(startDate: string, endDate: string): Observable<number> {
    return this.http
      .get<ApiResponse<number>>(`${this.apiUrl}/total`, {
        params: { startDate, endDate },
      })
      .pipe(map((r) => r.data));
  }

  createPayment(req: CreatePaymentRequest): Observable<PaymentResponse> {
    return this.http
      .post<ApiResponse<PaymentResponse>>(this.apiUrl, req)
      .pipe(map((r) => r.data));
  }

  getById(id: number): Observable<PaymentResponse> {
    return this.http
      .get<ApiResponse<PaymentResponse>>(`${this.apiUrl}/${id}`)
      .pipe(
        map((r) => r.data),
        tap((p) => this.currentPayment.set(p))
      );
  }
}