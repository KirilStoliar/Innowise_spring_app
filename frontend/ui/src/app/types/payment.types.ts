export type PaymentStatus = 'PENDING' | 'COMPLETED' | 'FAILED';

export interface Payment {
  id: number;
  orderId: number;
  userId: number;
  paymentAmount: number;
  status: PaymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentRequest {
  orderId: number;
  userId: number;
  paymentAmount: number;
}

export interface PaymentResponse {
  id: number;
  orderId: number;
  userId: number;
  status: PaymentStatus;
  timestamp: string;
  paymentAmount: number
}

export interface PaymentSearchParams {
  userId?: number;
  orderId?: number;
  status?: PaymentStatus;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}