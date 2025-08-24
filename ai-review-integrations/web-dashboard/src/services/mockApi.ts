import { PullRequest, CodeReview, ReviewFinding, ReviewSuggestion, DashboardMetrics } from './api'

// Mock data
const mockPullRequests: PullRequest[] = [
  {
    id: 1,
    title: "Add user authentication system",
    description: "Implement JWT-based authentication with role-based access control",
    author: "john.doe",
    status: "OPEN",
    branchName: "feature/auth-system",
    targetBranch: "main",
    createdAt: "2024-01-15T10:30:00Z",
    updatedAt: "2024-01-15T14:30:00Z"
  },
  {
    id: 2,
    title: "Fix memory leak in data processing",
    description: "Resolve memory issues in the batch processing service",
    author: "jane.smith",
    status: "MERGED",
    branchName: "fix/memory-leak",
    targetBranch: "main",
    createdAt: "2024-01-14T09:15:00Z",
    updatedAt: "2024-01-14T16:45:00Z"
  },
  {
    id: 3,
    title: "Update API documentation",
    description: "Add OpenAPI specifications and update existing endpoint docs",
    author: "bob.wilson",
    status: "DRAFT",
    branchName: "docs/api-update",
    targetBranch: "main",
    createdAt: "2024-01-13T14:20:00Z",
    updatedAt: "2024-01-13T14:20:00Z"
  }
]

const mockFindings: ReviewFinding[] = [
  {
    id: 1,
    severity: "HIGH",
    category: "Security",
    description: "SQL injection vulnerability detected",
    filePath: "src/main/java/UserService.java",
    lineNumber: 45,
    suggestion: "Use parameterized queries instead of string concatenation"
  },
  {
    id: 2,
    severity: "MEDIUM",
    category: "Performance",
    description: "Inefficient database query",
    filePath: "src/main/java/DataRepository.java",
    lineNumber: 78,
    suggestion: "Consider adding database index for better performance"
  }
]

const mockReviews: CodeReview[] = [
  {
    id: 1,
    pullRequestId: 1,
    reviewType: "AI",
    status: "CHANGES_REQUESTED",
    overallScore: 75,
    findings: mockFindings,
    createdAt: "2024-01-15T12:00:00Z"
  }
]

const mockSuggestions: ReviewSuggestion[] = [
  {
    id: 1,
    pullRequestId: 1,
    filePath: "src/main/java/AuthController.java",
    lineNumber: 23,
    category: "Security",
    severity: "HIGH",
    title: "Implement rate limiting",
    description: "Login endpoint should have rate limiting to prevent brute force attacks",
    suggestion: "Add @RateLimited annotation or implement rate limiting middleware",
    createdAt: "2024-01-15T12:00:00Z"
  },
  {
    id: 2,
    pullRequestId: 1,
    filePath: "src/main/java/TokenService.java",
    lineNumber: 56,
    category: "Security",
    severity: "MEDIUM",
    title: "Token expiration validation",
    description: "Add proper token expiration validation",
    suggestion: "Check token expiration before processing requests",
    createdAt: "2024-01-15T12:05:00Z"
  }
]

const mockDashboardMetrics: DashboardMetrics = {
  summary: {
    totalPullRequests: 128,
    averageReviewTime: 24.5,
    approvalRate: 85.2,
    reworkRate: 12.8
  },
  quality: {
    totalFindings: 45,
    criticalFindings: 3,
    highFindings: 8,
    securityFindings: 12
  },
  team: {
    activeAuthors: 15,
    activeReviewers: 8,
    totalReviews: 89,
    aiReviews: 67
  },
  trends: [
    { date: '2024-01-01', prs: 12, reviews: 15 },
    { date: '2024-01-02', prs: 8, reviews: 12 },
    { date: '2024-01-03', prs: 15, reviews: 18 },
    { date: '2024-01-04', prs: 10, reviews: 14 },
    { date: '2024-01-05', prs: 18, reviews: 22 },
    { date: '2024-01-06', prs: 14, reviews: 16 },
    { date: '2024-01-07', prs: 20, reviews: 25 }
  ]
}

// Mock API functions
export const mockApi = {
  pullRequests: {
    getAll: async (params?: { author?: string; status?: string }): Promise<{ data: PullRequest[] }> => {
      await new Promise(resolve => setTimeout(resolve, 500)) // Simulate network delay
      
      let filtered = mockPullRequests
      
      if (params?.author) {
        filtered = filtered.filter(pr => 
          pr.author.toLowerCase().includes(params.author!.toLowerCase())
        )
      }
      
      if (params?.status) {
        filtered = filtered.filter(pr => pr.status === params.status)
      }
      
      return { data: filtered }
    },
    
    getById: async (id: number): Promise<{ data: PullRequest }> => {
      await new Promise(resolve => setTimeout(resolve, 300))
      
      const pr = mockPullRequests.find(p => p.id === id)
      if (!pr) throw new Error('Pull request not found')
      
      return { data: pr }
    },
    
    triggerAiSuggestions: async (id: number, _filesToReview: string[]): Promise<{ data: ReviewSuggestion[] }> => {
      await new Promise(resolve => setTimeout(resolve, 2000)) // Simulate AI processing time
      return { data: mockSuggestions.filter(s => s.pullRequestId === id) }
    },
    
    getReviews: async (id: number): Promise<{ data: CodeReview[] }> => {
      await new Promise(resolve => setTimeout(resolve, 300))
      return { data: mockReviews.filter(r => r.pullRequestId === id) }
    }
  },
  
  metrics: {
    getDashboard: async (): Promise<{ data: DashboardMetrics }> => {
      await new Promise(resolve => setTimeout(resolve, 800))
      return { data: mockDashboardMetrics }
    }
  }
}