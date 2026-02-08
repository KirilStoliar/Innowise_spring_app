export interface PaymentCard {
    id: number;
    userId: number;
    number: string;
    holder: string;
    expirationDate: string;
    active: boolean;
    createdAt?: string;
    updatedAt?: string;
  }
  
  export interface PaymentCardCreateRequest {
    number: string;
    holder: string;
    expirationDate: string;
  }
  
  export interface UpdatePaymentCardRequest {
    number?: string;
    holder?: string;
    expirationDate?: string;
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