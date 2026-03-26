import { Component, HostListener } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-landing',
  imports: [RouterLink, CommonModule],
  templateUrl: './landing.html'
})
export class LandingComponent {
  leftEyeStyle = '';
  rightEyeStyle = '';
  menuOpen = false;

  @HostListener('document:mousemove', ['$event'])
  onMouseMove(e: MouseEvent): void {
    this.updateEyes(e.clientX, e.clientY);
  }

  @HostListener('document:touchmove', ['$event'])
  onTouchMove(e: TouchEvent): void {
    const t = e.touches[0];
    this.updateEyes(t.clientX, t.clientY);
  }

  private updateEyes(mx: number, my: number): void {
    this.leftEyeStyle  = this.pupilTransform(mx, my, 'left-eye');
    this.rightEyeStyle = this.pupilTransform(mx, my, 'right-eye');
  }

  private pupilTransform(mx: number, my: number, id: string): string {
    const el = document.getElementById(id);
    if (!el) return '';
    const r = el.getBoundingClientRect();
    const cx = r.left + r.width / 2;
    const cy = r.top + r.height / 2;
    const angle = Math.atan2(my - cy, mx - cx);
    const dist = Math.min(4, Math.hypot(mx - cx, my - cy) / 10);
    const x = Math.cos(angle) * dist;
    const y = Math.sin(angle) * dist;
    return `translate(${x}px, ${y}px)`;
  }
}
