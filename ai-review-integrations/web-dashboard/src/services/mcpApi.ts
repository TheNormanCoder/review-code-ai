import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
const USE_MOCK_API = true; // Force mock for demo

export const mcpApi = axios.create({
  baseURL: API_BASE_URL + '/mcp',
  headers: {
    'Content-Type': 'application/json',
  },
});

// MCP Types
export interface MCPTool {
  name: string;
  description: string;
  inputSchema: Record<string, any>;
}

export interface MCPSession {
  session_id: string;
  available_tools: MCPTool[];
  status: string;
}

export interface ReviewUpdate {
  status: string;
  content: any;
  error?: string;
  metadata?: Record<string, any>;
}

export interface EnhancedReviewResult {
  sessionId: string;
  success: boolean;
  error?: string;
  aiResponse?: {
    content: string;
    toolCalls?: ToolCall[];
    toolResults?: ToolResult[];
    metadata?: Record<string, any>;
  };
  toolResults?: ToolResult[];
  metadata?: Record<string, any>;
  timestamp: number;
}

export interface ToolCall {
  name: string;
  parameters: Record<string, any>;
}

export interface ToolResult {
  success: boolean;
  content: any;
  error?: string;
  mimeType?: string;
  metadata?: Record<string, any>;
  timestamp: string;
}

export interface AIExecutionRequest {
  prompt: string;
  toolNames: string[];
  context: Record<string, any>;
}

export interface ToolChainRequest {
  tools: Array<{
    name: string;
    parameters: Record<string, any>;
  }>;
}

// Mock data for MCP functionality
const mockMCPTools: MCPTool[] = [
  {
    name: 'git',
    description: 'Execute Git commands and analyze repository data',
    inputSchema: {
      type: 'object',
      properties: {
        command: { type: 'string', enum: ['diff', 'log', 'status', 'blame'] },
        repository: { type: 'string' },
        parameters: { type: 'object' }
      }
    }
  },
  {
    name: 'database',
    description: 'Query database for metrics and historical data',
    inputSchema: {
      type: 'object',
      properties: {
        query: { type: 'string', enum: ['pull_requests_summary', 'recent_reviews', 'security_findings'] },
        parameters: { type: 'object' }
      }
    }
  },
  {
    name: 'filesystem',
    description: 'Read files and analyze file structures',
    inputSchema: {
      type: 'object',
      properties: {
        operation: { type: 'string', enum: ['read_file', 'list_directory', 'find_files'] },
        path: { type: 'string' },
        parameters: { type: 'object' }
      }
    }
  },
  {
    name: 'notification',
    description: 'Send notifications via various channels',
    inputSchema: {
      type: 'object',
      properties: {
        channel: { type: 'string', enum: ['slack', 'teams', 'email'] },
        message: { type: 'string' },
        severity: { type: 'string', enum: ['info', 'warning', 'error', 'critical'] }
      }
    }
  }
];

const mockEnhancedReviewResult: EnhancedReviewResult = {
  sessionId: 'mock-session-123',
  success: true,
  aiResponse: {
    content: 'Comprehensive review completed using MCP tools. Found 3 security issues, 2 performance optimizations, and 5 code quality improvements.',
    toolCalls: [
      { name: 'git', parameters: { command: 'diff', repository: '/project' } },
      { name: 'database', parameters: { query: 'security_findings' } }
    ],
    toolResults: [
      {
        success: true,
        content: 'Git diff analyzed: 15 files changed, 234 insertions, 89 deletions',
        metadata: { files_changed: 15, insertions: 234, deletions: 89 },
        timestamp: new Date().toISOString()
      }
    ],
    metadata: { tools_used: 2, execution_time: 3500 }
  },
  toolResults: [],
  metadata: { enhanced: true, mcp_version: '2.0' },
  timestamp: Date.now()
};

// Mock API functions
const mockMCPApi = {
  sessions: {
    create: async (): Promise<MCPSession> => {
      await new Promise(resolve => setTimeout(resolve, 500));
      return {
        session_id: 'mock-session-' + Math.random().toString(36).substr(2, 9),
        available_tools: mockMCPTools,
        status: 'created'
      };
    },

    getActiveSessions: async (): Promise<{ active_sessions: number; sessions: Record<string, any> }> => {
      await new Promise(resolve => setTimeout(resolve, 300));
      return {
        active_sessions: 2,
        sessions: {
          'session-1': { session_id: 'session-1', is_active: true, context_size: 5 },
          'session-2': { session_id: 'session-2', is_active: true, context_size: 3 }
        }
      };
    },

    close: async (sessionId: string): Promise<{ status: string; session_id: string }> => {
      await new Promise(resolve => setTimeout(resolve, 200));
      return { status: 'closed', session_id: sessionId };
    }
  },

  tools: {
    getAvailable: async (): Promise<MCPTool[]> => {
      await new Promise(resolve => setTimeout(resolve, 300));
      return mockMCPTools;
    },

    execute: async (toolName: string, parameters: Record<string, any>): Promise<any> => {
      await new Promise(resolve => setTimeout(resolve, 1000));
      return {
        success: true,
        content: `Mock result from ${toolName} tool with parameters: ${JSON.stringify(parameters)}`,
        metadata: { tool: toolName, execution_time: 1000 }
      };
    },

    executeChain: async (request: ToolChainRequest): Promise<any[]> => {
      await new Promise(resolve => setTimeout(resolve, 2000));
      return request.tools.map(tool => ({
        success: true,
        content: `Mock result from ${tool.name}`,
        metadata: { tool: tool.name }
      }));
    }
  },

  ai: {
    executeWithTools: async (request: AIExecutionRequest): Promise<any> => {
      await new Promise(resolve => setTimeout(resolve, 3000));
      return {
        content: `AI analysis using tools: ${request.toolNames.join(', ')}. Prompt: "${request.prompt.substring(0, 50)}..."`,
        toolCalls: request.toolNames.map(name => ({ name, parameters: {} })),
        metadata: { tools_used: request.toolNames.length, prompt_length: request.prompt.length }
      };
    }
  },

  reviews: {
    performAdvanced: async (pullRequestId: number, options?: any): Promise<EnhancedReviewResult> => {
      await new Promise(resolve => setTimeout(resolve, 4000));
      return {
        ...mockEnhancedReviewResult,
        aiResponse: {
          ...mockEnhancedReviewResult.aiResponse!,
          content: `Advanced MCP review completed for PR #${pullRequestId}. Focus areas: ${options?.focusAreas?.join(', ') || 'security, performance, maintainability'}`
        }
      };
    },

    stream: (_pullRequestId: number, _options?: any) => {
      const updates = [
        { status: 'started', content: 'Starting MCP review session...', metadata: { stage: 1, total: 6 } },
        { status: 'analyzing', content: 'Analyzing code changes with Git tool...', metadata: { stage: 2, total: 6 } },
        { status: 'checking', content: 'Running security analysis...', metadata: { stage: 3, total: 6 } },
        { status: 'evaluating', content: 'Evaluating code quality...', metadata: { stage: 4, total: 6 } },
        { status: 'generating', content: 'Generating AI suggestions...', metadata: { stage: 5, total: 6 } },
        { status: 'completed', content: 'MCP review completed successfully', metadata: { stage: 6, total: 6, findings: 8 } }
      ];

      let onUpdateCallback: ((update: ReviewUpdate) => void) | null = null;
      let index = 0;

      const interval = setInterval(() => {
        if (index < updates.length && onUpdateCallback) {
          onUpdateCallback(updates[index]);
          index++;
        } else if (index >= updates.length) {
          clearInterval(interval);
        }
      }, 1000);

      return {
        onUpdate: (callback: (update: ReviewUpdate) => void) => {
          onUpdateCallback = callback;
        }
      };
    }
  }
};

// API functions with mock fallback
export const mcpApiService = {
  // Session management
  createSession: () => 
    USE_MOCK_API ? mockMCPApi.sessions.create() : mcpApi.post('/sessions').then(res => res.data),

  getActiveSessions: () =>
    USE_MOCK_API ? mockMCPApi.sessions.getActiveSessions() : mcpApi.get('/sessions').then(res => res.data),

  closeSession: (sessionId: string) =>
    USE_MOCK_API ? mockMCPApi.sessions.close(sessionId) : mcpApi.delete(`/sessions/${sessionId}`).then(res => res.data),

  // Tool management
  getAvailableTools: () =>
    USE_MOCK_API ? mockMCPApi.tools.getAvailable() : mcpApi.get('/tools').then(res => res.data),

  executeTool: (toolName: string, parameters: Record<string, any>) =>
    USE_MOCK_API ? mockMCPApi.tools.execute(toolName, parameters) : mcpApi.post(`/tools/${toolName}/execute`, parameters).then(res => res.data),

  executeToolChain: (request: ToolChainRequest) =>
    USE_MOCK_API ? mockMCPApi.tools.executeChain(request) : mcpApi.post('/tools/chain', request).then(res => res.data),

  // AI integration
  executeAIWithTools: (request: AIExecutionRequest) =>
    USE_MOCK_API ? mockMCPApi.ai.executeWithTools(request) : mcpApi.post('/ai/execute-with-tools', request).then(res => res.data),

  // Advanced reviews
  performAdvancedReview: (pullRequestId: number, options?: { focusAreas?: string[]; severityThreshold?: string }) =>
    USE_MOCK_API ? mockMCPApi.reviews.performAdvanced(pullRequestId, options) : mcpApi.post(`/reviews/advanced/${pullRequestId}`, options).then(res => res.data),

  streamReview: (pullRequestId: number, options?: { focusAreas?: string; severityThreshold?: string }) => {
    if (USE_MOCK_API) {
      return mockMCPApi.reviews.stream(pullRequestId, options);
    }
    
    // For real API, would use EventSource or WebSocket
    const eventSource = new EventSource(`${API_BASE_URL}/mcp/reviews/stream/${pullRequestId}?${new URLSearchParams(options as any)}`);
    return {
      onUpdate: (callback: (update: ReviewUpdate) => void) => {
        eventSource.onmessage = (event) => {
          callback(JSON.parse(event.data));
        };
      },
      close: () => eventSource.close()
    };
  }
};

// WebSocket client for real-time MCP communication
export class MCPWebSocketClient {
  private ws: WebSocket | null = null;
  private callbacks: Map<string, (data: any) => void> = new Map();

  connect(url: string = 'ws://localhost:8080/mcp-websocket') {
    if (USE_MOCK_API) {
      // Mock WebSocket for demo
      setTimeout(() => {
        this.callbacks.get('connection')?.(
          { type: 'connection', status: 'established', data: { session_id: 'mock-ws-session' } }
        );
      }, 1000);
      return;
    }

    this.ws = new WebSocket(url);
    
    this.ws.onopen = () => {
      console.log('MCP WebSocket connected');
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.callbacks.get(message.type)?.(message);
    };

    this.ws.onclose = () => {
      console.log('MCP WebSocket disconnected');
    };
  }

  send(message: any) {
    if (USE_MOCK_API) {
      // Mock send for demo
      setTimeout(() => {
        if (message.type === 'create_session') {
          this.callbacks.get('session_created')?.(
            { type: 'session_created', status: 'success', data: { mcp_session_id: 'mock-session', available_tools: mockMCPTools } }
          );
        }
      }, 500);
      return;
    }

    this.ws?.send(JSON.stringify(message));
  }

  on(event: string, callback: (data: any) => void) {
    this.callbacks.set(event, callback);
  }

  disconnect() {
    this.ws?.close();
  }
}