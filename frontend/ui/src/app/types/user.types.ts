export interface User {
  id: number;
  name: string;
  surname: string;
  birthDate: string;
  email: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserRequest {
  name?: string;
  surname?: string;
  birthDate?: string;
  email?: string;
}

export interface PageResponse<T> {
  data: {
    content: T[];
  };
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

export interface UserFilterParams {
  page?: number;
  size?: number;
  name?: string;
  surname?: string;
}
