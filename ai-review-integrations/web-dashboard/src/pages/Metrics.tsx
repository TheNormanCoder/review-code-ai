import { useQuery } from '@tanstack/react-query'
import { metricsApi } from '../services/api'
import { XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line } from 'recharts'
import { TrendingUp, TrendingDown, AlertTriangle, CheckCircle, Clock, Users } from 'lucide-react'


export default function Metrics() {
  const { data: dashboardMetrics, isLoading: dashboardLoading } = useQuery({
    queryKey: ['dashboard-metrics'],
    queryFn: () => metricsApi.getDashboard().then(res => res.data),
  })


  const { data: dailyData, isLoading: dailyLoading } = useQuery({
    queryKey: ['daily-metrics'],
    queryFn: () => metricsApi.getDaily(14).then(res => res.data),
  })

  if (dashboardLoading) {
    return (
      <div className="animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/4 mb-4"></div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="bg-white p-6 rounded-lg shadow-sm border h-32"></div>
          ))}
        </div>
      </div>
    )
  }

  const qualityData = dashboardMetrics ? [
    { name: 'Critical', value: dashboardMetrics.quality.criticalFindings, color: '#ef4444' },
    { name: 'High', value: dashboardMetrics.quality.highFindings, color: '#f59e0b' },
    { name: 'Medium', value: Math.max(0, dashboardMetrics.quality.totalFindings - dashboardMetrics.quality.criticalFindings - dashboardMetrics.quality.highFindings - dashboardMetrics.quality.securityFindings), color: '#3b82f6' },
    { name: 'Security', value: dashboardMetrics.quality.securityFindings, color: '#8b5cf6' },
  ].filter(item => item.value > 0) : []

  return (
    <div>
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Metrics Dashboard</h1>
        <p className="mt-2 text-gray-600">Comprehensive view of code review performance and quality</p>
      </div>

      {/* Key Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Average Review Time</p>
              <p className="text-2xl font-bold text-gray-900">
                {dashboardMetrics?.summary.averageReviewTime?.toFixed(1) || 0}h
              </p>
            </div>
            <Clock className="w-8 h-8 text-blue-600" />
          </div>
          <div className="mt-4 flex items-center text-sm">
            <TrendingDown className="w-4 h-4 text-green-600 mr-1" />
            <span className="text-green-600">12% faster than last week</span>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Approval Rate</p>
              <p className="text-2xl font-bold text-gray-900">
                {dashboardMetrics?.summary.approvalRate?.toFixed(1) || 0}%
              </p>
            </div>
            <CheckCircle className="w-8 h-8 text-green-600" />
          </div>
          <div className="mt-4 flex items-center text-sm">
            <TrendingUp className="w-4 h-4 text-green-600 mr-1" />
            <span className="text-green-600">5% improvement</span>
          </div>
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium text-gray-600">Rework Rate</p>
              <p className="text-2xl font-bold text-gray-900">
                {dashboardMetrics?.summary.reworkRate?.toFixed(1) || 0}%
              </p>
            </div>
            <AlertTriangle className="w-8 h-8 text-orange-600" />
          </div>
          <div className="mt-4 flex items-center text-sm">
            <TrendingDown className="w-4 h-4 text-green-600 mr-1" />
            <span className="text-green-600">3% reduction</span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
        {/* Quality Distribution */}
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Issue Distribution</h2>
          </div>
          <div className="p-6">
            {qualityData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={qualityData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, value }) => `${name}: ${value}`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {qualityData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex items-center justify-center h-64 text-gray-500">
                No quality data available
              </div>
            )}
          </div>
        </div>

        {/* Team Activity */}
        <div className="bg-white rounded-lg shadow-sm border">
          <div className="px-6 py-4 border-b border-gray-200">
            <h2 className="text-lg font-semibold text-gray-900">Team Activity</h2>
          </div>
          <div className="p-6">
            <div className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <Users className="w-5 h-5 text-blue-600 mr-3" />
                  <span className="text-gray-700">Active Authors</span>
                </div>
                <span className="text-2xl font-bold text-gray-900">
                  {dashboardMetrics?.team.activeAuthors || 0}
                </span>
              </div>
              
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <Users className="w-5 h-5 text-green-600 mr-3" />
                  <span className="text-gray-700">Active Reviewers</span>
                </div>
                <span className="text-2xl font-bold text-gray-900">
                  {dashboardMetrics?.team.activeReviewers || 0}
                </span>
              </div>
              
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <CheckCircle className="w-5 h-5 text-purple-600 mr-3" />
                  <span className="text-gray-700">Total Reviews</span>
                </div>
                <span className="text-2xl font-bold text-gray-900">
                  {dashboardMetrics?.team.totalReviews || 0}
                </span>
              </div>
              
              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <div className="w-5 h-5 bg-blue-600 rounded mr-3 flex items-center justify-center">
                    <span className="text-xs text-white font-bold">AI</span>
                  </div>
                  <span className="text-gray-700">AI Reviews</span>
                </div>
                <span className="text-2xl font-bold text-gray-900">
                  {dashboardMetrics?.team.aiReviews || 0}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Trends Chart */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">Activity Trends</h2>
        </div>
        <div className="p-6">
          {!dailyLoading && dailyData?.length > 0 ? (
            <ResponsiveContainer width="100%" height={400}>
              <LineChart data={dailyData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="period" 
                  tickFormatter={(value) => new Date(value).toLocaleDateString()}
                />
                <YAxis />
                <Tooltip 
                  labelFormatter={(value) => new Date(value).toLocaleDateString()}
                />
                <Line 
                  type="monotone" 
                  dataKey="totalPullRequests" 
                  stroke="#3b82f6" 
                  strokeWidth={2}
                  name="Pull Requests"
                />
                <Line 
                  type="monotone" 
                  dataKey="totalReviews" 
                  stroke="#10b981" 
                  strokeWidth={2}
                  name="Reviews"
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-96 text-gray-500">
              {dailyLoading ? 'Loading trends...' : 'No trend data available'}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}