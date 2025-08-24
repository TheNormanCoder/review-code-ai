import axios from 'axios'
import { mockApi } from './mockApi'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'
const USE_MOCK_API = !import.meta.env.VITE_API_URL // Use mock when no API URL is provided

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

export interface PullRequest {
  id: number
  title: string
  description: string
  author: string
  status: 'OPEN' | 'CLOSED' | 'MERGED' | 'DRAFT'
  branchName: string
  targetBranch: string
  createdAt: string
  updatedAt: string
}

export interface CodeReview {
  id: number
  pullRequestId: number
  reviewType: 'AI' | 'HUMAN'
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CHANGES_REQUESTED'
  overallScore: number
  findings: ReviewFinding[]
  createdAt: string
}

export interface ReviewFinding {
  id: number
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  category: string
  description: string
  filePath: string
  lineNumber: number
  suggestion: string
}

export interface ReviewSuggestion {
  id: number
  pullRequestId: number
  filePath: string
  lineNumber: number
  category: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  title: string
  description: string
  suggestion: string
  createdAt: string
}

export interface DashboardMetrics {
  summary: {
    totalPullRequests: number
    averageReviewTime: number
    approvalRate: number
    reworkRate: number
  }
  quality: {
    totalFindings: number
    criticalFindings: number
    highFindings: number
    securityFindings: number
  }
  team: {
    activeAuthors: number
    activeReviewers: number
    totalReviews: number
    aiReviews: number
  }
  trends: any[]
}

// API functions
export const pullRequestsApi = {
  getAll: (params?: { author?: string; status?: string }) =>
    USE_MOCK_API ? mockApi.pullRequests.getAll(params) : api.get<PullRequest[]>('/reviews/pull-requests', { params }),
  
  getById: (id: number) =>
    USE_MOCK_API ? mockApi.pullRequests.getById(id) : api.get<PullRequest>(`/reviews/pull-requests/${id}`),
  
  create: (data: Omit<PullRequest, 'id' | 'createdAt' | 'updatedAt'>) =>
    api.post<PullRequest>('/reviews/pull-requests', data),
  
  triggerAiSuggestions: (id: number, filesToReview: string[]) =>
    USE_MOCK_API ? mockApi.pullRequests.triggerAiSuggestions(id, filesToReview) : api.post<ReviewSuggestion[]>(`/reviews/pull-requests/${id}/ai-suggestions`, filesToReview),
  
  triggerFinalReview: (id: number, filesToReview: string[]) =>
    api.post<CodeReview>(`/reviews/pull-requests/${id}/ai-final-review`, filesToReview),
  
  getReviews: (id: number) =>
    USE_MOCK_API ? mockApi.pullRequests.getReviews(id) : api.get<CodeReview[]>(`/reviews/pull-requests/${id}/reviews`),
  
  addHumanReview: (id: number, review: Omit<CodeReview, 'id' | 'createdAt'>) =>
    api.post<CodeReview>(`/reviews/pull-requests/${id}/human-review`, review),
}

export const metricsApi = {
  getDashboard: () =>
    USE_MOCK_API ? mockApi.metrics.getDashboard() : api.get<DashboardMetrics>('/reviews/metrics/dashboard'),
  
  getDaily: (days = 30) =>
    api.get('/reviews/metrics/daily', { params: { days } }),
  
  getWeekly: (weeks = 12) =>
    api.get('/reviews/metrics/weekly', { params: { weeks } }),
  
  getMonthly: (months = 12) =>
    api.get('/reviews/metrics/monthly', { params: { months } }),
}