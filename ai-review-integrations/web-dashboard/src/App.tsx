import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import PullRequests from './pages/PullRequests'
import PullRequestDetail from './pages/PullRequestDetail'
import Metrics from './pages/Metrics'

function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/pull-requests" element={<PullRequests />} />
        <Route path="/pull-requests/:id" element={<PullRequestDetail />} />
        <Route path="/metrics" element={<Metrics />} />
      </Routes>
    </Layout>
  )
}

export default App