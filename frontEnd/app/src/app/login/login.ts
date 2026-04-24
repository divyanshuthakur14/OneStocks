import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  identifier = '';
  password = '';
  rememberMe = false;
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly isLoading = signal(false);

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit(): void {
    this.errorMessage.set('');
    this.successMessage.set('');

    if (!this.identifier || !this.password) {
      this.errorMessage.set('All fields are required.');
      return;
    }

    this.isLoading.set(true);

    this.authService.login({
      identifier: this.identifier,
      password: this.password,
      rememberMe: this.rememberMe
    }).subscribe({
      next: (response) => {
        this.isLoading.set(false);
        if (response.success) {
          this.successMessage.set(`Welcome back, ${response.username}! Redirecting...`);
          setTimeout(() => this.router.navigate(['/home']), 1500);
        } else {
          this.errorMessage.set(response.message || 'Invalid credentials. Please try again.');
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Invalid credentials. Please try again.');
      }
    });
  }
}