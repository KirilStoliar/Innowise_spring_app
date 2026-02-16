import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { User, UpdateUserRequest, PageResponse, UserFilterParams } from '../types/user.types';

interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiGatewayUrl}/api/v1/users`;

  readonly users = signal<User[]>([]);
  readonly currentUser = signal<User | null>(null);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly userId = signal<number | null>(null);

  getAll(params?: PageRequest): Observable<any> {
    let httpParams = new HttpParams();
    if (params?.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params?.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params?.sort) httpParams = httpParams.set('sort', params.sort);

    return this.http.get<any>(this.apiUrl, { params: httpParams }).pipe(
      tap((res) => {
        this.users.set(res.data.content);
        this.totalPages.set(res.data.totalPages);
        this.totalElements.set(res.totalElements);
      })
    );
  }

  getUserIdByEmail(email: string): Observable<{
    success: boolean;
    message: string;
    data: number;
    timestamp: string;
  }> {
    return this.http.get<any>(`${this.apiUrl}/email/${email}`);
  }

  getAllWithFilters(params: UserFilterParams): Observable<PageResponse<User>> {
    let httpParams = new HttpParams();
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    if (params.name) httpParams = httpParams.set('name', params.name);
    if (params.surname) httpParams = httpParams.set('surname', params.surname);

    return this.http.get<PageResponse<User>>(`${this.apiUrl}/filter`, { params: httpParams }).pipe(
      tap((res) => {
        this.users.set(res.data.content);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
      })
    );
  }

  getById(id: number): Observable<User> {
    return this.http
      .get<User>(`${this.apiUrl}/${id}`)
      .pipe(tap((user) => this.currentUser.set(user)));
  }

  setUserId(userId: number) {
    this.userId.set(userId);
  }

  update(id: number, req: UpdateUserRequest): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  updateStatus(id: number, active: boolean): Observable<User> {
    return this.http.patch<User>(`${this.apiUrl}/${id}/status`, null, {
      params: { active },
    });
  }
}
