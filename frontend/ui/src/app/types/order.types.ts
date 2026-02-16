export interface Item {
  id: number;
  name: string;
  price: number;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  id: number;
  itemId: number;
  quantity: number;
  itemName: string;
  itemPrice: number;
}

export interface UserInfo {
  id: number;
  name: string;
  surname: string;
  birthDate: string;
  email: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface Order {
  id: number;
  userId: number;
  userEmail: string;
  status: OrderStatus;
  totalPrice: number;
  createdAt: string;
  updatedAt: string;
  orderItems: OrderItem[];
  userInfo?: UserInfo;
}

export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

export interface Pageable {
  pageNumber: number;
  pageSize: number;
  sort: { unsorted: boolean; sorted: boolean; empty: boolean };
  offset: number;
  unpaged: boolean;
  paged: boolean;
}

export interface PageResponse<T> {
  content: T[];
  pageable: Pageable;
  totalElements: number;
  totalPages: number;
  last: boolean;
  numberOfElements: number;
  size: number;
  number: number;
  first: boolean;
  empty: boolean;
}

export interface CreateOrderRequest {
  userId?: number;
  orderItems: { itemId: number; quantity: number }[];
}

export interface UpdateOrderRequest {
  status?: OrderStatus;
  userID: number;
  orderItems?: { id: number; itemId: number; quantity: number }[];
}

export interface CreateOrderItemRequest {
  orderId: number;
  itemId: number;
  quantity: number;
}

export interface UpdateOrderItemRequest {
  quantity: number;
}
