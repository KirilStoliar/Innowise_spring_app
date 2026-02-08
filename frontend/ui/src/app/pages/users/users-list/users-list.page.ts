import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { User } from '../../../types/user.types';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './users-list.page.html',
  styleUrl: './users-list.page.css',
})
export class UsersListPage implements OnInit {
  readonly auth = inject(AuthService);
  readonly userService = inject(UserService);
  private router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly users = signal<User[]>([]);
  readonly currentPage = signal(0);

  filterName = '';
  filterSurname = '';

  constructor() {
    effect(() => {
      const total = this.userService.totalPages();
      console.log(total);
    });
  }

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.loading.set(true);
    this.error.set(null);

    const hasFilters = this.filterName || this.filterSurname;

    const params = {
      page: this.currentPage(),
      size: 10,
      ...(this.filterName && { name: this.filterName }),
      ...(this.filterSurname && { surname: this.filterSurname }),
    };

    const request$ = hasFilters
      ? this.userService.getAllWithFilters(params)
      : this.userService.getAll({ page: params.page, size: params.size });

    request$.subscribe({
      next: (res) => {
        this.users.set(res.data.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load users');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  applyFilters() {
    this.currentPage.set(0);
    this.loadUsers();
  }

  resetFilters() {
    this.filterName = '';
    this.filterSurname = '';
    this.currentPage.set(0);
    this.loadUsers();
  }

  changePage(page: number) {
    this.currentPage.set(page);
    this.loadUsers();
  }

  toggleStatus(user: User) {
    if (
      !confirm(
        `Are you sure you want to ${user.active ? 'deactivate' : 'activate'} ${user.name} ${
          user.surname
        }?`
      )
    ) {
      return;
    }

    this.userService.updateStatus(user.id, !user.active).subscribe({
      next: () => this.loadUsers(),
      error: (err) => {
        this.error.set('Failed to update user status');
        console.error(err);
      },
    });
  }

  navigateTo(path: string) {
    this.router.navigate([path]);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/auth']);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  addUser() {}
}
