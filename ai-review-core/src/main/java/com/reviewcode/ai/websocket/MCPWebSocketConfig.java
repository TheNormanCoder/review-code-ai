package com.reviewcode.ai.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration for real-time MCP communication
 */
@Configuration
@EnableWebSocket
public class MCPWebSocketConfig implements WebSocketConfigurer {
    
    private final MCPWebSocketHandler mcpWebSocketHandler;
    
    public MCPWebSocketConfig(MCPWebSocketHandler mcpWebSocketHandler) {
        this.mcpWebSocketHandler = mcpWebSocketHandler;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mcpWebSocketHandler, "/mcp-websocket")
                .setAllowedOrigins("*") // Configure properly for production
                .withSockJS(); // Enable SockJS fallback
    }
}