import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, switchMap } from 'rxjs';

import { ApiResponse, LoginRequest, TokenValidationResponse } from '../../types/auth.types';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-auth',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './auth.page.html',
  styleUrls: ['./auth.page.css'],
})
export class Auth {
  private auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private router = inject(Router);

  title = computed(() => 'Sign in');

  isSubmitting = signal(false);
  errorText = signal<string | null>(null);

  loginForm = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control('admin@example.com', [
      Validators.required,
      Validators.email,
    ]),
    password: this.fb.nonNullable.control('admin123', [
      Validators.required,
    ]),
  });

  submitLogin() {
    this.errorText.set(null);

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    const req: LoginRequest = this.loginForm.getRawValue();

    this.isSubmitting.set(true);
    this.auth
      .login(req)
      .pipe(
        switchMap((res) => {
          if (!res.success) throw new Error(res.message || 'Login failed');
          this.auth.setTokens(res.data);
          return this.auth.validate(res.data.accessToken);
        }),
        finalize(() => this.isSubmitting.set(false))
      )
      .subscribe({
        next: (vres: ApiResponse<TokenValidationResponse>) => {
          if (!vres.success || !vres.data?.valid) {
            this.errorText.set(vres.message || 'Token is invalid');
            return;
          }
          this.auth.setIdentity(vres.data);
          this.router.navigate(['orders']);
        },
        error: (err) => {
          this.errorText.set(err?.error?.message ?? err?.message ?? 'Login failed');
        },
      });
  }
}
