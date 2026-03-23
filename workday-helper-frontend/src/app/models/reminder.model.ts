export interface Reminder {
  id?: number;
  message: string;
  type: 'EYE_BREAK' | 'POSTURE' | 'HYDRATION' | 'STRETCH' | 'CUSTOM';
  intervalMinutes: number;
  active: boolean;
  lastTriggered?: string;
  createdAt?: string;
}
