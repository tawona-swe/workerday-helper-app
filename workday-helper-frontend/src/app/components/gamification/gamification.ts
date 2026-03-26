import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

interface Achievement {
  id: number;
  name: string;
  description?: string;
  unlockedAt?: string;
}

interface GamificationProfile {
  totalPoints: number;
  currentStreak: number;
  longestStreak: number;
  achievements: Achievement[];
}

@Component({
  selector: 'app-gamification',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './gamification.html'
})
export class GamificationComponent implements OnInit {
  profile: GamificationProfile | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<GamificationProfile>(`${environment.apiUrl}/api/gamification/profile`).subscribe({
      next: p => this.profile = p,
      error: () => {}
    });
  }
}
