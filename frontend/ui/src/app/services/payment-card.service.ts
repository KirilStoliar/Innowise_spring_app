import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  PaymentCard,
  UpdatePaymentCardRequest,
  PageResponse,
  PaymentCardCreateRequest,
} from '../types/payment-card.types';
import { ApiResponse } from '../types/auth.types';

interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentCardService {
  private http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiGatewayUrl || ''}/api/v1/users`;

  readonly cards = signal<PaymentCard[]>([]);
  readonly currentCard = signal<PaymentCard | null>(null);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);

  // GET /api/v1/users/{userId}/payment-cards  -> ApiResponse<List<PaymentCardDTO>>
  getCardsByUserId(userId: number): Observable<PaymentCard[]> {
    return this.http
      .get<ApiResponse<PaymentCard[]>>(`${this.apiUrl}/${userId}/payment-cards`)
      .pipe(map((res) => res.data));
  }

  // GET /api/v1/users/{userId}/payment-cards/paged -> ApiResponse<Page<PaymentCardDTO>>
  getCardsByUserIdPaged(
    userId: number,
    params?: PageRequest
  ): Observable<PageResponse<PaymentCard>> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', String(params.page));
    if (params?.size !== undefined) httpParams = httpParams.set('size', String(params.size));
    if (params?.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http
      .get<ApiResponse<PageResponse<PaymentCard>>>(`${this.apiUrl}/${userId}/payment-cards/paged`, {
        params: httpParams,
      })
      .pipe(
        map((res) => res.data),
        tap((page) => {
          this.cards.set(page.content);
          this.totalPages.set(page.totalPages);
          this.totalElements.set(page.totalElements);
        })
      );
  }

  create(userId: number, req: PaymentCardCreateRequest): Observable<PaymentCard> {
    return this.http
      .post<ApiResponse<PaymentCard>>(`${this.apiUrl}/${userId}/payment-cards`, req)
      .pipe(map((res) => res.data));
  }

  update(userId: number, cardId: number, req: UpdatePaymentCardRequest): Observable<PaymentCard> {
    return this.http
      .put<ApiResponse<PaymentCard>>(`${this.apiUrl}/${userId}/payment-cards/${cardId}`, req)
      .pipe(map((res) => res.data));
  }

  updateStatus(userId: number, cardId: number, active: boolean): Observable<PaymentCard> {
    return this.http
      .patch<ApiResponse<PaymentCard>>(
        `${this.apiUrl}/${userId}/payment-cards/${cardId}/status`,
        null,
        { params: { active: String(active) } }
      )
      .pipe(map((res) => res.data));
  }

  delete(userId: number, cardId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}/payment-cards/${cardId}`);
  }
}
