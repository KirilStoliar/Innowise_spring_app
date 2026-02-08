import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  FormsModule,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {
  ApiResponse,
  RegisterUserResponse,
  Role,
  UserCredentialsRequest,
} from '../../../types/auth.types';
import { AuthService } from '../../../services/auth.service';
import { finalize } from 'rxjs';

export function dateBeforeToday(): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const value = (control.value ?? '').trim();
    if (!value) return null;

    const d = new Date(value + 'T00:00:00');
    if (isNaN(d.getTime())) return { invalidDate: true };

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    d.setHours(0, 0, 0, 0);

    return d < today ? null : { dateNotBeforeToday: true };
  };
}

@Component({
  selector: 'app-user-form.page',
  imports: [CommonModule, RouterLink, FormsModule, ReactiveFormsModule],
  templateUrl: './user-form.page.html',
  styleUrl: './user-form.page.css',
})
export class UserFormPage implements OnInit {
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  fb = inject(FormBuilder);
  auth = inject(AuthService);
  router = inject(Router);

  isSubmitting = signal(false);

  registerUserForm = this.fb.nonNullable.group({
    email: this.fb.nonNullable.control('example@test.com', [Validators.required, Validators.email]),
    name: this.fb.nonNullable.control('Name', [Validators.required]),
    surname: this.fb.nonNullable.control('Surename', [Validators.required]),
    birthDate: this.fb.nonNullable.control('2000-01-01', [Validators.required, dateBeforeToday()]),
    role: this.fb.nonNullable.control<Role>('USER', [Validators.required]),
    password: this.fb.nonNullable.control('root', [Validators.required]),
  });

  ngOnInit(): void {
    this.success.set(null);
    this.error.set(null);
  }

  submitRegistration() {
    if (this.registerUserForm.invalid) {
      this.registerUserForm.markAllAsTouched();
      return;
    }

    const req: UserCredentialsRequest = this.registerUserForm.getRawValue();

    this.isSubmitting.set(true);
    this.auth
      .registerByAdmin(req)
      .pipe(
        finalize(() => {
          this.isSubmitting.set(false);
        })
      )
      .subscribe({
        next: (value: ApiResponse<RegisterUserResponse>) => {
          this.success.set('Registration Completed successfully');
          this.router.navigate(['users']);
        },
        error: (err) => {
          this.error.set(err?.error?.message ?? err?.message ?? 'Registration failed');
        },
      });
  }
}
