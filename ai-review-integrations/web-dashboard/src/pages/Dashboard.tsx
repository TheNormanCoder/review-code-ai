import { useQuery } from '@tanstack/react-query'
import { metricsApi, pullRequestsApi } from '../services/api'
import { GitPullRequest, TrendingUp, Users, AlertTriangle } from 'lucide-react'
import { Link } from 'react-router-dom'

export default function Dashboard() {
  const { data: metrics, isLoading: metricsLoading } = useQuery({
    queryKey: ['dashboard-metrics'],
    queryFn: () => metricsApi.getDashboard().then(res => res.data),
  })

  const { data: recentPRs, isLoading: prsLoading } = useQuery({
    queryKey: ['recent-prs'],
    queryFn: () => pullRequestsApi.getAll().then(res => res.data.slice(0, 5)),
  })

  if (metricsLoading) {
    return (
      <div className="animate-pulse">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="bg-white p-6 rounded-lg shadow-sm border h-24"></div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <p className="mt-2 text-gray-600">Overview of code review metrics and activity</p>
      </div>

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <GitPullRequest className="w-8 h-8 text-blue-600" />
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total PRs</p>
              <p className="text-2xl font-bold text-gray-900">
                {metrics?.summary.totalPullRequests || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <TrendingUp className="w-8 h-8 text-green-600" />
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Approval Rate</p>
              <p className="text-2xl font-bold text-gray-900">
                {metrics?.summary.approvalRate?.toFixed(1) || 0}%
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <Users className="w-8 h-8 text-purple-600" />
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Active Authors</p>
              <p className="text-2xl font-bold text-gray-900">
                {metrics?.team.activeAuthors || 0}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center">
            <AlertTriangle className="w-8 h-8 text-red-600" />
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Critical Issues</p>
              <p className="text-2xl font-bold text-gray-900">
                {metrics?.quality.criticalFindings || 0}
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent Pull Requests */}
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Recent Pull Requests</h2>
          </div>
          <div className="p-6">
            {prsLoading ? (
              <div className="space-y-3">
                {[...Array(3)].map((_, i) => (
                  <div key={i} className="animate-pulse flex space-x-4">
                    <div className="rounded-full bg-gray-200 h-10 w-10"></div>
                    <div className="flex-1 space-y-2 py-1">
                      <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                      <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                    </div>
                  </div>
                ))}
              </div>
            ) : recentPRs?.length ? (
              <div className="space-y-4">
                {recentPRs.map((pr) => (
                  <Link
                    key={pr.id}
                    to={`/pull-requests/${pr.id}`}
                    className="block hover:bg-gray-50 rounded-lg p-3 -m-3 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <p className="text-sm font-medium text-gray-900">{pr.title}</p>
                        <p className="text-xs text-gray-500">by {pr.author}</p>
                      </div>
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          pr.status === 'OPEN'
                            ? 'bg-green-100 text-green-800'
                            : pr.status === 'MERGED'
                            ? 'bg-purple-100 text-purple-800'
                            : 'bg-gray-100 text-gray-800'
                        }`}
                      >
                        {pr.status.toLowerCase()}
                      </span>
                    </div>
                  </Link>
                ))}
              </div>
            ) : (
              <p className="text-gray-500">No pull requests found</p>
            )}
            <div className="mt-4 pt-4 border-t">
              <Link
                to="/pull-requests"
                className="text-sm text-blue-600 hover:text-blue-700 font-medium"
              >
                View all pull requests →
              </Link>
            </div>
          </div>
        </div>

        {/* Quality Insights */}
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Quality Insights</h2>
          </div>
          <div className="p-6">
            {metrics ? (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Total Findings</span>
                  <span className="text-sm font-semibold">{metrics.quality.totalFindings}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">High Priority</span>
                  <span className="text-sm font-semibold text-orange-600">{metrics.quality.highFindings}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">Security Issues</span>
                  <span className="text-sm font-semibold text-red-600">{metrics.quality.securityFindings}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-600">AI Reviews</span>
                  <span className="text-sm font-semibold text-blue-600">{metrics.team.aiReviews}</span>
                </div>
              </div>
            ) : (
              <p className="text-gray-500">Loading quality metrics...</p>
            )}
            <div className="mt-4 pt-4 border-t">
              <Link
                to="/metrics"
                className="text-sm text-blue-600 hover:text-blue-700 font-medium"
              >
                View detailed metrics →
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}