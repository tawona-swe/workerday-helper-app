export interface Task {
  id?: number;
  title: string;
  description: string;
  completed: boolean;
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  dueDate?: string;
  createdAt?: string;
}
