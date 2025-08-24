import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { mcpApiService, MCPTool, EnhancedReviewResult, ReviewUpdate } from '../services/mcpApi';
import { Zap, Settings, Play, StopCircle, CheckCircle, AlertTriangle, Info, Database, GitBranch, FileText, Bell } from 'lucide-react';

interface MCPReviewPanelProps {
  pullRequestId: number;
  onReviewComplete?: (result: EnhancedReviewResult) => void;
}

export default function MCPReviewPanel({ pullRequestId, onReviewComplete }: MCPReviewPanelProps) {
  const [selectedTools, setSelectedTools] = useState<string[]>(['git', 'database', 'filesystem']);
  const [focusAreas, setFocusAreas] = useState<string[]>(['security', 'performance', 'maintainability']);
  const [severityThreshold, setSeverityThreshold] = useState('medium');
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamUpdates, setStreamUpdates] = useState<ReviewUpdate[]>([]);

  // Fetch available MCP tools
  const { data: availableTools, isLoading: toolsLoading } = useQuery({
    queryKey: ['mcp-tools'],
    queryFn: () => mcpApiService.getAvailableTools(),
  });

  // Advanced review mutation
  const advancedReview = useMutation({
    mutationFn: (options: { focusAreas: string[]; severityThreshold: string }) =>
      mcpApiService.performAdvancedReview(pullRequestId, options),
    onSuccess: (result) => {
      onReviewComplete?.(result);
    },
  });

  // Stream review
  const startStreamReview = () => {
    setIsStreaming(true);
    setStreamUpdates([]);
    
    const stream = mcpApiService.streamReview(pullRequestId, {
      focusAreas: focusAreas.join(','),
      severityThreshold
    });

    stream.onUpdate?.((update: ReviewUpdate) => {
      setStreamUpdates(prev => [...prev, update]);
      
      if (update.status === 'completed') {
        setIsStreaming(false);
      }
    });
  };

  const stopStreamReview = () => {
    setIsStreaming(false);
  };

  const runAdvancedReview = () => {
    advancedReview.mutate({
      focusAreas,
      severityThreshold
    });
  };

  const getToolIcon = (toolName: string) => {
    switch (toolName) {
      case 'git': return <GitBranch className="w-4 h-4" />;
      case 'database': return <Database className="w-4 h-4" />;
      case 'filesystem': return <FileText className="w-4 h-4" />;
      case 'notification': return <Bell className="w-4 h-4" />;
      default: return <Settings className="w-4 h-4" />;
    }
  };

  const getUpdateIcon = (status: string) => {
    switch (status) {
      case 'completed': return <CheckCircle className="w-4 h-4 text-green-500" />;
      case 'error': return <AlertTriangle className="w-4 h-4 text-red-500" />;
      default: return <Info className="w-4 h-4 text-blue-500" />;
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-2">
          <Zap className="w-5 h-5 text-purple-500" />
          <h3 className="text-lg font-semibold">MCP Enhanced Review</h3>
          <span className="bg-purple-100 text-purple-800 text-xs px-2 py-1 rounded-full">
            AI + Tools
          </span>
        </div>
      </div>

      {/* Tool Selection */}
      <div className="mb-6">
        <h4 className="text-sm font-medium text-gray-700 mb-3">Available MCP Tools</h4>
        {toolsLoading ? (
          <div className="space-y-2">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="animate-pulse bg-gray-200 h-12 rounded"></div>
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {availableTools?.map((tool: MCPTool) => (
              <div
                key={tool.name}
                className={`border rounded-lg p-3 cursor-pointer transition-colors ${
                  selectedTools.includes(tool.name)
                    ? 'border-purple-500 bg-purple-50'
                    : 'border-gray-200 hover:border-gray-300'
                }`}
                onClick={() => {
                  setSelectedTools(prev =>
                    prev.includes(tool.name)
                      ? prev.filter(t => t !== tool.name)
                      : [...prev, tool.name]
                  );
                }}
              >
                <div className="flex items-start space-x-2">
                  {getToolIcon(tool.name)}
                  <div className="flex-1">
                    <div className="font-medium text-sm">{tool.name}</div>
                    <div className="text-xs text-gray-500 mt-1">{tool.description}</div>
                  </div>
                  {selectedTools.includes(tool.name) && (
                    <CheckCircle className="w-4 h-4 text-purple-500" />
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Review Options */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Focus Areas
          </label>
          <div className="space-y-2">
            {['security', 'performance', 'maintainability', 'best_practices', 'architecture'].map(area => (
              <label key={area} className="flex items-center">
                <input
                  type="checkbox"
                  checked={focusAreas.includes(area)}
                  onChange={(e) => {
                    if (e.target.checked) {
                      setFocusAreas(prev => [...prev, area]);
                    } else {
                      setFocusAreas(prev => prev.filter(a => a !== area));
                    }
                  }}
                  className="rounded border-gray-300 text-purple-600 focus:ring-purple-500"
                />
                <span className="ml-2 text-sm text-gray-700 capitalize">
                  {area.replace('_', ' ')}
                </span>
              </label>
            ))}
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Severity Threshold
          </label>
          <select
            value={severityThreshold}
            onChange={(e) => setSeverityThreshold(e.target.value)}
            className="w-full rounded-lg border-gray-300 shadow-sm focus:border-purple-500 focus:ring-purple-500"
          >
            <option value="low">Low - Show all findings</option>
            <option value="medium">Medium - Important issues only</option>
            <option value="high">High - Critical issues only</option>
          </select>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex items-center space-x-3 mb-6">
        <button
          onClick={runAdvancedReview}
          disabled={advancedReview.isPending || selectedTools.length === 0}
          className="btn btn-primary flex items-center space-x-2"
        >
          <Play className="w-4 h-4" />
          <span>
            {advancedReview.isPending ? 'Running Review...' : 'Run Advanced Review'}
          </span>
        </button>

        <button
          onClick={isStreaming ? stopStreamReview : startStreamReview}
          disabled={selectedTools.length === 0}
          className="btn btn-secondary flex items-center space-x-2"
        >
          {isStreaming ? <StopCircle className="w-4 h-4" /> : <Play className="w-4 h-4" />}
          <span>
            {isStreaming ? 'Stop Stream' : 'Stream Review'}
          </span>
        </button>
      </div>

      {/* Stream Updates */}
      {streamUpdates.length > 0 && (
        <div className="bg-gray-50 rounded-lg p-4 mb-6">
          <h5 className="text-sm font-medium text-gray-700 mb-3 flex items-center">
            <Info className="w-4 h-4 mr-2" />
            Real-time Updates
          </h5>
          <div className="space-y-2 max-h-48 overflow-y-auto">
            {streamUpdates.map((update, index) => (
              <div key={index} className="flex items-start space-x-2 text-sm">
                {getUpdateIcon(update.status)}
                <div className="flex-1">
                  <span className="text-gray-700">{update.content}</span>
                  {update.metadata && (
                    <div className="text-xs text-gray-500 mt-1">
                      {update.metadata.stage && (
                        <span>Stage {update.metadata.stage}/{update.metadata.total} </span>
                      )}
                      {update.metadata.findings && (
                        <span>• {update.metadata.findings} findings</span>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Review Results */}
      {advancedReview.data && (
        <div className="bg-green-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-3">
            <CheckCircle className="w-5 h-5 text-green-500" />
            <h5 className="font-medium text-green-800">Review Complete</h5>
          </div>
          
          <div className="text-sm text-green-700 mb-3">
            {advancedReview.data.aiResponse?.content}
          </div>

          {advancedReview.data.aiResponse?.toolResults && (
            <div className="space-y-2">
              <h6 className="text-xs font-medium text-green-800">Tool Results:</h6>
              {advancedReview.data.aiResponse.toolResults.map((result: any, index: number) => (
                <div key={index} className="text-xs text-green-600 bg-white rounded p-2">
                  <strong>Tool Result {index + 1}:</strong> {result.content}
                </div>
              ))}
            </div>
          )}

          {advancedReview.data.metadata && (
            <div className="mt-3 text-xs text-green-600">
              <strong>Session ID:</strong> {advancedReview.data.sessionId}<br/>
              <strong>Timestamp:</strong> {new Date(advancedReview.data.timestamp).toLocaleString()}
            </div>
          )}
        </div>
      )}

      {advancedReview.error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center space-x-2">
            <AlertTriangle className="w-5 h-5 text-red-500" />
            <h5 className="font-medium text-red-800">Review Failed</h5>
          </div>
          <div className="text-sm text-red-700 mt-2">
            {(advancedReview.error as Error).message}
          </div>
        </div>
      )}
    </div>
  );
}