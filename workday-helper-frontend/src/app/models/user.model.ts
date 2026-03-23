export interface User {
  id: number;
  name: string;
  email: string;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}
