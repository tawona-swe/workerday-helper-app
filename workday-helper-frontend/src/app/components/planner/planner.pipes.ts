import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'taskCount', standalone: true })
export class TaskCountPipe implements PipeTransform {
  transform(plan: any[]): number { return plan.filter(b => b.type === 'task').length; }
}

@Pipe({ name: 'breakCount', standalone: true })
export class BreakCountPipe implements PipeTransform {
  transform(plan: any[]): number { return plan.filter(b => b.type === 'break').length; }
}

@Pipe({ name: 'highPriorityCount', standalone: true })
export class HighPriorityCountPipe implements PipeTransform {
  transform(plan: any[]): number { return plan.filter(b => b.priority === 'HIGH').length; }
}
