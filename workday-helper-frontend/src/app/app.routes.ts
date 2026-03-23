import { Routes } from '@angular/router';
import { AuthLayoutComponent } from './layouts/auth-layout';
import { AppLayoutComponent } from './layouts/app-layout';
import { DashboardComponent } from './components/dashboard/dashboard';
import { TasksComponent } from './components/tasks/tasks';
import { RemindersComponent } from './components/reminders/reminders';
import { LoginComponent } from './components/auth/login';
import { RegisterComponent } from './components/auth/register';
import { ForgotPasswordComponent } from './components/auth/forgot-password';
import { AccountComponent } from './components/auth/account';
import { authGuard } from './auth/auth.guard';

export const routes: Routes = [
  // Root redirect
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Auth pages — wrapped in AuthLayoutComponent (no sidebar/topbar)
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      { path: 'login',           component: LoginComponent },
      { path: 'register',        component: RegisterComponent },
      { path: 'forgot-password', component: ForgotPasswordComponent },
    ]
  },

  // App shell — all protected pages live inside AppLayoutComponent
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'tasks',     component: TasksComponent },
      { path: 'reminders', component: RemindersComponent },
      { path: 'account',   component: AccountComponent },
    ]
  }
];
