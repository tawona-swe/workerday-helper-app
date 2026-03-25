import { Routes } from '@angular/router';
import { AuthLayoutComponent } from './layouts/auth-layout';
import { AppLayoutComponent } from './layouts/app-layout';
import { LandingComponent } from './components/landing/landing';
import { DashboardComponent } from './components/dashboard/dashboard';
import { TasksComponent } from './components/tasks/tasks';
import { RemindersComponent } from './components/reminders/reminders';
import { LoginComponent } from './components/auth/login';
import { RegisterComponent } from './components/auth/register';
import { ForgotPasswordComponent } from './components/auth/forgot-password';
import { AccountComponent } from './components/auth/account';
import { authGuard } from './auth/auth.guard';
import { ChatComponent } from './components/chat/chat';
import { FocusComponent } from './components/focus/focus';
import { SuggestionsComponent } from './components/suggestions/suggestions';
import { AnalyticsDashboardComponent } from './components/analytics/analytics';
import { GamificationComponent } from './components/gamification/gamification';

export const routes: Routes = [
  // Landing page
  { path: '', component: LandingComponent, pathMatch: 'full' },

  // Auth pages
  {
    path: '',
    component: AuthLayoutComponent,
    children: [
      { path: 'login',           component: LoginComponent },
      { path: 'register',        component: RegisterComponent },
      { path: 'forgot-password', component: ForgotPasswordComponent },
    ]
  },

  // App shell — all protected pages
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard',    component: DashboardComponent },
      { path: 'tasks',        component: TasksComponent },
      { path: 'reminders',    component: RemindersComponent },
      { path: 'account',      component: AccountComponent },
      { path: 'chat',         component: ChatComponent },
      { path: 'focus',        component: FocusComponent },
      { path: 'suggestions',  component: SuggestionsComponent },
      { path: 'analytics',    component: AnalyticsDashboardComponent },
      { path: 'gamification', component: GamificationComponent },
    ]
  }
];
