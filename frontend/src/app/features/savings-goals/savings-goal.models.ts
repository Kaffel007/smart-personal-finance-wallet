export interface SavingsGoal {
  id: number;
  name: string;
  targetAmount: number;
  savedAmount: number;
  remainingAmount: number;
  progressPercent: number;
  targetDate: string | null;
}

export interface CreateSavingsGoalRequest {
  name: string;
  targetAmount: number;
  savedAmount?: number | null;
  targetDate?: string | null;
}

export interface UpdateSavingsGoalRequest {
  name: string;
  targetAmount: number;
  savedAmount: number;
  targetDate?: string | null;
}
