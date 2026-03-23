import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet],
  template: `
    <div class="app-bg min-h-screen">
      <router-outlet />
    </div>
  `
})
export class AuthLayoutComponent {}
