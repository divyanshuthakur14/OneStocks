import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs/operators';
import { AuthService } from '../../auth';

@Component({
  selector: 'app-header',
  imports: [RouterModule],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);


  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(e => (e as NavigationEnd).urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

  showHeader = computed(() => {
    const url = this.currentUrl() ?? '';
    return !url.includes('/login') && !url.includes('/signup');
  });

  readonly username = signal<string | null>(null);

  constructor() {
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe(() => {
      this.username.set(this.authService.getUsername());
    });
  }

  ngOnInit(): void {
    this.username.set(this.authService.getUsername());
  }

  get userInitial(): string{
    return this.username()?.charAt(0).toUpperCase() ?? "P";
  }

  logout(): void {
    this.authService.logout().subscribe(() => {
      this.router.navigate(['/login']);
    });
  }
}