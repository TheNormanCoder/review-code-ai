import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { pullRequestsApi, PullRequest } from '../services/api'
import { Link } from 'react-router-dom'
import { GitPullRequest, Calendar, User, Zap, RefreshCw } from 'lucide-react'

export default function PullRequests() {
  const [statusFilter, setStatusFilter] = useState<string>('')
  const [authorFilter, setAuthorFilter] = useState<string>('')
  const queryClient = useQueryClient()

  const { data: pullRequests, isLoading, error } = useQuery({
    queryKey: ['pull-requests', statusFilter, authorFilter],
    queryFn: () => pullRequestsApi.getAll({
      ...(statusFilter && { status: statusFilter }),
      ...(authorFilter && { author: authorFilter }),
    }).then(res => res.data),
  })

  const triggerAiReview = useMutation({
    mutationFn: ({ id, files }: { id: number; files: string[] }) =>
      pullRequestsApi.triggerAiSuggestions(id, files),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pull-requests'] })
    },
  })

  const handleAiReview = (pr: PullRequest) => {
    // For demo, we'll assume some common files to review
    const filesToReview = ['src/main/java/com/example/Service.java']
    triggerAiReview.mutate({ id: pr.id, files: filesToReview })
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'OPEN': return 'bg-green-100 text-green-800'
      case 'MERGED': return 'bg-purple-100 text-purple-800'
      case 'CLOSED': return 'bg-gray-100 text-gray-800'
      case 'DRAFT': return 'bg-yellow-100 text-yellow-800'
      default: return 'bg-gray-100 text-gray-800'
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  if (error) {
    return (
      <div className="text-center py-12">
        <div className="text-red-600 mb-4">Failed to load pull requests</div>
        <button 
          onClick={() => window.location.reload()}
          className="btn btn-primary"
        >
          Retry
        </button>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Pull Requests</h1>
        <p className="mt-2 text-gray-600">Manage and review pull requests with AI assistance</p>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Status</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
            >
              <option value="">All statuses</option>
              <option value="OPEN">Open</option>
              <option value="DRAFT">Draft</option>
              <option value="MERGED">Merged</option>
              <option value="CLOSED">Closed</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Author</label>
            <input
              type="text"
              value={authorFilter}
              onChange={(e) => setAuthorFilter(e.target.value)}
              placeholder="Filter by author..."
              className="w-full rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
            />
          </div>
          <div className="flex items-end">
            <button
              onClick={() => {
                setStatusFilter('')
                setAuthorFilter('')
              }}
              className="btn btn-secondary"
            >
              Clear Filters
            </button>
          </div>
        </div>
      </div>

      {/* Pull Requests List */}
      <div className="bg-white rounded-lg shadow-sm border">
        {isLoading ? (
          <div className="p-6">
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="animate-pulse">
                  <div className="flex space-x-4">
                    <div className="rounded-full bg-gray-200 h-10 w-10"></div>
                    <div className="flex-1 space-y-2 py-1">
                      <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                      <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ) : pullRequests?.length ? (
          <div className="divide-y divide-gray-200">
            {pullRequests.map((pr) => (
              <div key={pr.id} className="p-6 hover:bg-gray-50 transition-colors">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center space-x-3 mb-2">
                      <GitPullRequest className="w-5 h-5 text-gray-400" />
                      <Link
                        to={`/pull-requests/${pr.id}`}
                        className="text-lg font-medium text-gray-900 hover:text-blue-600"
                      >
                        {pr.title}
                      </Link>
                      <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(pr.status)}`}>
                        {pr.status.toLowerCase()}
                      </span>
                    </div>
                    
                    {pr.description && (
                      <p className="text-gray-600 mb-3 line-clamp-2">{pr.description}</p>
                    )}
                    
                    <div className="flex items-center space-x-6 text-sm text-gray-500">
                      <div className="flex items-center">
                        <User className="w-4 h-4 mr-1" />
                        {pr.author}
                      </div>
                      <div className="flex items-center">
                        <Calendar className="w-4 h-4 mr-1" />
                        {formatDate(pr.createdAt)}
                      </div>
                      <div>
                        {pr.branchName} → {pr.targetBranch}
                      </div>
                    </div>
                  </div>
                  
                  <div className="flex items-center space-x-3 ml-6">
                    <button
                      onClick={() => handleAiReview(pr)}
                      disabled={triggerAiReview.isPending}
                      className="btn btn-primary flex items-center space-x-2"
                    >
                      {triggerAiReview.isPending ? (
                        <RefreshCw className="w-4 h-4 animate-spin" />
                      ) : (
                        <Zap className="w-4 h-4" />
                      )}
                      <span>AI Review</span>
                    </button>
                    <Link
                      to={`/pull-requests/${pr.id}`}
                      className="btn btn-secondary"
                    >
                      View Details
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-12">
            <GitPullRequest className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-500">No pull requests found</p>
            <p className="text-sm text-gray-400 mt-2">
              {statusFilter || authorFilter
                ? 'Try adjusting your filters'
                : 'Pull requests will appear here when created'}
            </p>
          </div>
        )}
      </div>
    </div>
  )
}