import { useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { pullRequestsApi } from '../services/api'
import MCPReviewPanel from '../components/MCPReviewPanel'
import { EnhancedReviewResult } from '../services/mcpApi'
import { 
  ArrowLeft, 
  GitBranch, 
  Calendar, 
  User, 
  Zap, 
  CheckCircle, 
  XCircle, 
  AlertTriangle, 
  Info,
  RefreshCw,
  FileText,
  Code
} from 'lucide-react'

export default function PullRequestDetail() {
  const { id } = useParams<{ id: string }>()
  const [selectedFiles, setSelectedFiles] = useState<string[]>([])
  const [showMCPPanel, setShowMCPPanel] = useState(false)
  const queryClient = useQueryClient()

  const { data: pullRequest, isLoading: prLoading } = useQuery({
    queryKey: ['pull-request', id],
    queryFn: () => pullRequestsApi.getById(Number(id!)).then(res => res.data),
    enabled: !!id,
  })

  const { data: reviews, isLoading: reviewsLoading } = useQuery({
    queryKey: ['pull-request-reviews', id],
    queryFn: () => pullRequestsApi.getReviews(Number(id!)).then(res => res.data),
    enabled: !!id,
  })

  const triggerAiSuggestions = useMutation({
    mutationFn: (files: string[]) =>
      pullRequestsApi.triggerAiSuggestions(Number(id!), files),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pull-request-reviews', id] })
    },
  })

  const triggerFinalReview = useMutation({
    mutationFn: (files: string[]) =>
      pullRequestsApi.triggerFinalReview(Number(id!), files),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['pull-request-reviews', id] })
    },
  })

  const handleAiSuggestions = () => {
    const files = selectedFiles.length > 0 ? selectedFiles : [
      'src/main/java/com/example/Service.java',
      'src/main/java/com/example/Controller.java'
    ]
    triggerAiSuggestions.mutate(files)
  }

  const handleFinalReview = () => {
    const files = selectedFiles.length > 0 ? selectedFiles : [
      'src/main/java/com/example/Service.java',
      'src/main/java/com/example/Controller.java'
    ]
    triggerFinalReview.mutate(files)
  }

  const getSeverityIcon = (severity: string) => {
    switch (severity.toLowerCase()) {
      case 'critical':
        return <XCircle className="w-5 h-5 text-red-600" />
      case 'high':
        return <AlertTriangle className="w-5 h-5 text-orange-600" />
      case 'medium':
        return <Info className="w-5 h-5 text-blue-600" />
      case 'low':
        return <CheckCircle className="w-5 h-5 text-green-600" />
      default:
        return <Info className="w-5 h-5 text-gray-600" />
    }
  }

  const getSeverityColor = (severity: string) => {
    switch (severity.toLowerCase()) {
      case 'critical':
        return 'bg-red-100 text-red-800 border-red-200'
      case 'high':
        return 'bg-orange-100 text-orange-800 border-orange-200'
      case 'medium':
        return 'bg-blue-100 text-blue-800 border-blue-200'
      case 'low':
        return 'bg-green-100 text-green-800 border-green-200'
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200'
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString()
  }

  const handleMCPReviewComplete = (result: EnhancedReviewResult) => {
    // Invalidate queries to refresh the data
    queryClient.invalidateQueries({ queryKey: ['pull-request-reviews', id] })
    
    // Show success message or handle the result as needed
    console.log('MCP Review completed:', result)
  }

  if (prLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/4 mb-4"></div>
        <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
          <div className="h-6 bg-gray-200 rounded w-3/4 mb-4"></div>
          <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        </div>
      </div>
    )
  }

  if (!pullRequest) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Pull request not found</p>
        <Link to="/pull-requests" className="btn btn-primary mt-4">
          Back to Pull Requests
        </Link>
      </div>
    )
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <Link 
          to="/pull-requests" 
          className="inline-flex items-center text-blue-600 hover:text-blue-700 mb-4"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Pull Requests
        </Link>
        <h1 className="text-3xl font-bold text-gray-900">{pullRequest.title}</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* PR Info */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="flex items-center justify-between mb-4">
              <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${
                pullRequest.status === 'OPEN' ? 'bg-green-100 text-green-800' :
                pullRequest.status === 'MERGED' ? 'bg-purple-100 text-purple-800' :
                'bg-gray-100 text-gray-800'
              }`}>
                {pullRequest.status.toLowerCase()}
              </span>
              <span className="text-sm text-gray-500">#{pullRequest.id}</span>
            </div>
            
            {pullRequest.description && (
              <div className="mb-6">
                <h3 className="text-lg font-medium text-gray-900 mb-2">Description</h3>
                <p className="text-gray-700 whitespace-pre-wrap">{pullRequest.description}</p>
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
              <div className="flex items-center">
                <User className="w-4 h-4 mr-2" />
                Author: {pullRequest.author}
              </div>
              <div className="flex items-center">
                <Calendar className="w-4 h-4 mr-2" />
                Created: {formatDate(pullRequest.createdAt)}
              </div>
              <div className="flex items-center">
                <GitBranch className="w-4 h-4 mr-2" />
                {pullRequest.branchName} → {pullRequest.targetBranch}
              </div>
              <div className="flex items-center">
                <Calendar className="w-4 h-4 mr-2" />
                Updated: {formatDate(pullRequest.updatedAt)}
              </div>
            </div>
          </div>

          {/* AI Review Actions */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">AI Review Actions</h3>
            
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Files to Review (optional)
              </label>
              <textarea
                value={selectedFiles.join('\n')}
                onChange={(e) => setSelectedFiles(e.target.value.split('\n').filter(f => f.trim()))}
                placeholder="src/main/java/com/example/Service.java&#10;src/main/java/com/example/Controller.java"
                className="w-full h-24 rounded-lg border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500"
              />
              <p className="text-xs text-gray-500 mt-1">
                Leave empty to review common files automatically
              </p>
            </div>

            <div className="flex flex-wrap gap-3">
              <button
                onClick={handleAiSuggestions}
                disabled={triggerAiSuggestions.isPending}
                className="btn btn-primary flex items-center space-x-2"
              >
                {triggerAiSuggestions.isPending ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Zap className="w-4 h-4" />
                )}
                <span>Get AI Suggestions</span>
              </button>
              
              <button
                onClick={() => setShowMCPPanel(!showMCPPanel)}
                className="btn btn-secondary flex items-center space-x-2 bg-purple-600 text-white hover:bg-purple-700"
              >
                <Zap className="w-4 h-4" />
                <span>{showMCPPanel ? 'Hide MCP Panel' : 'MCP Enhanced Review'}</span>
              </button>
              
              <button
                onClick={handleFinalReview}
                disabled={triggerFinalReview.isPending}
                className="btn btn-secondary flex items-center space-x-2"
              >
                {triggerFinalReview.isPending ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <CheckCircle className="w-4 h-4" />
                )}
                <span>Final AI Review</span>
              </button>
            </div>
          </div>

          {/* MCP Enhanced Review Panel */}
          {showMCPPanel && (
            <MCPReviewPanel
              pullRequestId={Number(id!)}
              onReviewComplete={handleMCPReviewComplete}
            />
          )}

          {/* Reviews */}
          <div className="bg-white rounded-lg shadow-sm border">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-medium text-gray-900">Reviews & Findings</h3>
            </div>
            
            {reviewsLoading ? (
              <div className="p-6">
                <div className="animate-pulse space-y-4">
                  {[...Array(2)].map((_, i) => (
                    <div key={i} className="border rounded-lg p-4">
                      <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
                      <div className="h-3 bg-gray-200 rounded w-3/4"></div>
                    </div>
                  ))}
                </div>
              </div>
            ) : reviews?.length ? (
              <div className="divide-y divide-gray-200">
                {reviews.map((review) => (
                  <div key={review.id} className="p-6">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center space-x-3">
                        <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${
                          review.reviewType === 'AI' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800'
                        }`}>
                          {review.reviewType}
                        </span>
                        <span className={`inline-flex items-center px-2 py-1 rounded text-xs font-medium ${
                          review.status === 'APPROVED' ? 'bg-green-100 text-green-800' :
                          review.status === 'REJECTED' ? 'bg-red-100 text-red-800' :
                          'bg-yellow-100 text-yellow-800'
                        }`}>
                          {review.status}
                        </span>
                        {review.overallScore !== undefined && (
                          <span className="text-sm text-gray-600">
                            Score: {review.overallScore}/10
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-gray-500">
                        {formatDate(review.createdAt)}
                      </span>
                    </div>
                    
                    {review.findings && review.findings.length > 0 && (
                      <div className="space-y-3">
                        {review.findings.map((finding) => (
                          <div
                            key={finding.id}
                            className={`border rounded-lg p-4 ${getSeverityColor(finding.severity)}`}
                          >
                            <div className="flex items-start space-x-3">
                              {getSeverityIcon(finding.severity)}
                              <div className="flex-1">
                                <div className="flex items-center justify-between mb-2">
                                  <h4 className="font-medium">{finding.category}</h4>
                                  <span className="text-xs opacity-75">
                                    {finding.filePath}:{finding.lineNumber}
                                  </span>
                                </div>
                                <p className="text-sm mb-2">{finding.description}</p>
                                {finding.suggestion && (
                                  <div className="mt-2 p-2 bg-white bg-opacity-50 rounded">
                                    <p className="text-xs font-medium mb-1">Suggestion:</p>
                                    <p className="text-sm">{finding.suggestion}</p>
                                  </div>
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-6 text-center text-gray-500">
                <FileText className="w-8 h-8 mx-auto mb-2 opacity-50" />
                <p>No reviews yet</p>
                <p className="text-sm mt-1">Trigger an AI review to get started</p>
              </div>
            )}
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Quick Actions */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">Quick Actions</h3>
            <div className="space-y-3">
              <button className="w-full btn btn-primary flex items-center justify-center space-x-2">
                <Code className="w-4 h-4" />
                <span>View Code</span>
              </button>
              <button className="w-full btn btn-secondary flex items-center justify-center space-x-2">
                <FileText className="w-4 h-4" />
                <span>View Diff</span>
              </button>
            </div>
          </div>

          {/* Summary Stats */}
          {reviews && reviews.length > 0 && (
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h3 className="text-lg font-medium text-gray-900 mb-4">Review Summary</h3>
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Total Reviews:</span>
                  <span className="font-medium">{reviews.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">AI Reviews:</span>
                  <span className="font-medium">
                    {reviews.filter(r => r.reviewType === 'AI').length}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Total Findings:</span>
                  <span className="font-medium">
                    {reviews.reduce((acc, r) => acc + (r.findings?.length || 0), 0)}
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}