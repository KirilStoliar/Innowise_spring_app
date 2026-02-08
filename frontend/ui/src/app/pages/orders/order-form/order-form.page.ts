import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../services/order.service';
import { ItemService } from '../../../services/item.service';
import { AuthService } from '../../../services/auth.service';
import { Item } from '../../../types/order.types';
import { UserService } from '../../../services/user.service';

interface OrderItemForm {
  itemId: number;
  itemName: string;
  quantity: number;
  price: number;
}

@Component({
  selector: 'app-order-form',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './order-form.page.html',
  styleUrl: './order-form.page.css',
})
export class OrderFormPage implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private orderService = inject(OrderService);
  private itemService = inject(ItemService);
  private userService = inject(UserService);
  private auth = inject(AuthService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly isEditMode = signal(false);
  readonly orderId = signal<number | null>(null);
  readonly availableItems = signal<Item[]>([]);
  readonly orderItems = signal<OrderItemForm[]>([]);

  selectedItemId = '';
  selectedQuantity = 1;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode.set(true);
      this.orderId.set(+id);
      this.loadOrder(+id);
    }

    this.loadItems();
  }

  loadItems() {
    this.itemService.getAll().subscribe({
      next: (items) => this.availableItems.set(items),
      error: (err) => {
        this.error.set('Failed to load items');
        console.error(err);
      },
    });
  }

  loadOrder(id: number) {
    this.loading.set(true);
    this.orderService.getById(id).subscribe({
      next: (order) => {
        const items = order.orderItems.map((item) => ({
          itemId: item.itemId,
          itemName: item.itemName,
          quantity: item.quantity,
          price: item.itemPrice,
        }));
        this.orderItems.set(items);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load order');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  addItem() {
    if (!this.selectedItemId || this.selectedQuantity < 1) return;

    const item = this.availableItems().find((i) => i.id === +this.selectedItemId);
    if (!item) return;

    const existing = this.orderItems().find((i) => i.itemId === item.id);
    if (existing) {
      // Update quantity
      this.orderItems.update((items) =>
        items.map((i) =>
          i.itemId === item.id ? { ...i, quantity: i.quantity + this.selectedQuantity } : i
        )
      );
    } else {
      // Add new
      this.orderItems.update((items) => [
        ...items,
        {
          itemId: item.id,
          itemName: item.name,
          quantity: this.selectedQuantity,
          price: item.price,
        },
      ]);
    }

    this.selectedItemId = '';
    this.selectedQuantity = 1;
  }

  removeItem(itemId: number) {
    this.orderItems.update((items) => items.filter((i) => i.itemId !== itemId));
  }

  calculateTotal(): number {
    return this.orderItems().reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  save() {
    if (this.orderItems().length === 0) return;

    this.loading.set(true);
    this.error.set(null);

    const request = {
      userId: this.userService.userId() ?? 1,
      orderItems: this.orderItems().map((item, indx) => ({
        id: indx + 1,
        itemId: item.itemId,
        quantity: item.quantity,
      })),
    };

    const save$ = this.isEditMode()
      ? this.orderService.update(this.orderId()!, {
          status: 'PENDING',
          userID: this.userService.userId() ?? 1,
          orderItems: request.orderItems,
        })
      : this.orderService.create(request);

    save$.subscribe({
      next: () => {
        this.router.navigate(['/orders']);
      },
      error: (err) => {
        this.error.set('Failed to save order');
        this.loading.set(false);
        console.error(err);
      },
    });
  }
}
