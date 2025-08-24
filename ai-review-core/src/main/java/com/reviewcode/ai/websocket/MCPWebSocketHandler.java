package com.reviewcode.ai.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewcode.ai.mcp.EnhancedMCPClient;
import com.reviewcode.ai.mcp.MCPSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time MCP communication
 * Supports streaming AI responses and tool execution updates
 */
@Component
public class MCPWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private EnhancedMCPClient mcpClient;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, MCPSession> mcpSessions = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        
        // Send connection acknowledgment
        WebSocketMessage welcomeMessage = WebSocketMessage.builder()
            .type("connection")
            .status("established")
            .data(Map.of("session_id", session.getId()))
            .build();
        
        sendMessage(session, welcomeMessage);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            WebSocketMessage request = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
            handleMCPMessage(session, request);
            
        } catch (Exception e) {
            WebSocketMessage errorMessage = WebSocketMessage.builder()
                .type("error")
                .status("failed")
                .error("Invalid message format: " + e.getMessage())
                .build();
            
            sendMessage(session, errorMessage);
        }
    }
    
    private void handleMCPMessage(WebSocketSession session, WebSocketMessage request) {
        switch (request.getType()) {
            case "create_session" -> handleCreateSession(session, request);
            case "execute_tool" -> handleExecuteTool(session, request);
            case "stream_review" -> handleStreamReview(session, request);
            case "close_session" -> handleCloseSession(session, request);
            default -> {
                WebSocketMessage errorMessage = WebSocketMessage.builder()
                    .type("error")
                    .status("failed")
                    .error("Unknown message type: " + request.getType())
                    .build();
                sendMessage(session, errorMessage);
            }
        }
    }
    
    private void handleCreateSession(WebSocketSession session, WebSocketMessage request) {
        try {
            MCPSession mcpSession = mcpClient.createSession(session.getId());
            mcpSessions.put(session.getId(), mcpSession);
            
            WebSocketMessage response = WebSocketMessage.builder()
                .type("session_created")
                .status("success")
                .data(Map.of(
                    "mcp_session_id", mcpSession.getSessionId(),
                    "available_tools", mcpSession.getAvailableToolsSchema()
                ))
                .build();
            
            sendMessage(session, response);
            
        } catch (Exception e) {
            WebSocketMessage errorMessage = WebSocketMessage.builder()
                .type("session_error")
                .status("failed")
                .error("Failed to create MCP session: " + e.getMessage())
                .build();
            
            sendMessage(session, errorMessage);
        }
    }
    
    private void handleExecuteTool(WebSocketSession session, WebSocketMessage request) {
        MCPSession mcpSession = mcpSessions.get(session.getId());
        if (mcpSession == null) {
            sendError(session, "No active MCP session");
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> toolRequest = (Map<String, Object>) request.getData();
            String toolName = (String) toolRequest.get("tool");
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters = (Map<String, Object>) toolRequest.get("parameters");
            
            mcpSession.executeTool(toolName, parameters)
                .subscribe(
                    result -> {
                        WebSocketMessage response = WebSocketMessage.builder()
                            .type("tool_result")
                            .status(result.isSuccess() ? "success" : "error")
                            .data(result.getContent())
                            .error(result.getError())
                            .metadata(result.getMetadata())
                            .build();
                        
                        sendMessage(session, response);
                    },
                    error -> sendError(session, "Tool execution failed: " + error.getMessage())
                );
                
        } catch (Exception e) {
            sendError(session, "Invalid tool request: " + e.getMessage());
        }
    }
    
    private void handleStreamReview(WebSocketSession session, WebSocketMessage request) {
        MCPSession mcpSession = mcpSessions.get(session.getId());
        if (mcpSession == null) {
            sendError(session, "No active MCP session");
            return;
        }
        
        try {
            // This would integrate with the streaming review functionality
            // For demo, we'll simulate streaming updates
            simulateStreamingReview(session);
            
        } catch (Exception e) {
            sendError(session, "Failed to start streaming review: " + e.getMessage());
        }
    }
    
    private void handleCloseSession(WebSocketSession session, WebSocketMessage request) {
        MCPSession mcpSession = mcpSessions.remove(session.getId());
        if (mcpSession != null) {
            mcpSession.close();
            
            WebSocketMessage response = WebSocketMessage.builder()
                .type("session_closed")
                .status("success")
                .build();
            
            sendMessage(session, response);
        }
    }
    
    private void simulateStreamingReview(WebSocketSession session) {
        // Simulate streaming review updates
        String[] stages = {
            "Initializing review session...",
            "Analyzing code changes...",
            "Running security checks...",
            "Evaluating code quality...",
            "Generating suggestions...",
            "Review completed!"
        };
        
        for (int i = 0; i < stages.length; i++) {
            final int stage = i;
            
            // Simulate delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000 * stage);
                    
                    WebSocketMessage update = WebSocketMessage.builder()
                        .type("review_update")
                        .status("in_progress")
                        .data(Map.of(
                            "stage", stage + 1,
                            "total_stages", stages.length,
                            "message", stages[stage],
                            "progress", (stage + 1) * 100 / stages.length
                        ))
                        .build();
                    
                    sendMessage(session, update);
                    
                    if (stage == stages.length - 1) {
                        WebSocketMessage completion = WebSocketMessage.builder()
                            .type("review_complete")
                            .status("success")
                            .data(Map.of(
                                "findings", 5,
                                "critical", 1,
                                "suggestions", 8
                            ))
                            .build();
                        
                        sendMessage(session, completion);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
    
    private void sendError(WebSocketSession session, String error) {
        WebSocketMessage errorMessage = WebSocketMessage.builder()
            .type("error")
            .status("failed")
            .error(error)
            .build();
        
        sendMessage(session, errorMessage);
    }
    
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            
        } catch (IOException e) {
            System.err.println("Failed to send WebSocket message: " + e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        
        MCPSession mcpSession = mcpSessions.remove(session.getId());
        if (mcpSession != null) {
            mcpSession.close();
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
        session.close();
    }
    
    /**
     * WebSocket message format
     */
    public static class WebSocketMessage {
        private String type;
        private String status;
        private Object data;
        private String error;
        private Map<String, Object> metadata;
        private long timestamp;
        
        private WebSocketMessage() {
            this.timestamp = System.currentTimeMillis();
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public static class Builder {
            private WebSocketMessage message = new WebSocketMessage();
            
            public Builder type(String type) { message.type = type; return this; }
            public Builder status(String status) { message.status = status; return this; }
            public Builder data(Object data) { message.data = data; return this; }
            public Builder error(String error) { message.error = error; return this; }
            public Builder metadata(Map<String, Object> metadata) { message.metadata = metadata; return this; }
            
            public WebSocketMessage build() { return message; }
        }
    }
}