package com.reviewcode.ai.mcp;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification tool for MCP
 * Sends notifications via various channels (Slack, Teams, Email, etc.)
 */
@Component
public class NotificationTool implements MCPTool {
    
    @Override
    public String getName() {
        return "notification";
    }
    
    @Override
    public String getDescription() {
        return "Send notifications about code review results, critical findings, or important updates to various channels like Slack, Teams, or email.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "channel", Map.of(
                "type", "string",
                "description", "Notification channel to use",
                "enum", List.of("slack", "teams", "email", "webhook", "console")
            ),
            "message", Map.of(
                "type", "string",
                "description", "Notification message content"
            ),
            "severity", Map.of(
                "type", "string",
                "description", "Notification severity level",
                "enum", List.of("info", "warning", "error", "critical"),
                "default", "info"
            ),
            "parameters", Map.of(
                "type", "object",
                "description", "Channel-specific parameters",
                "properties", Map.of(
                    "webhook_url", Map.of("type", "string", "description", "Webhook URL for notifications"),
                    "channel_id", Map.of("type", "string", "description", "Channel or room ID"),
                    "recipients", Map.of("type", "array", "items", Map.of("type", "string"), "description", "Email recipients"),
                    "title", Map.of("type", "string", "description", "Notification title"),
                    "pull_request_id", Map.of("type", "integer", "description", "Related PR ID"),
                    "findings", Map.of("type", "array", "description", "Review findings to include")
                )
            )
        ));
        schema.put("required", List.of("channel", "message"));
        return schema;
    }
    
    @Override
    public Mono<MCPToolResult> execute(Map<String, Object> parameters) {
        String channel = (String) parameters.get("channel");
        String message = (String) parameters.get("message");
        String severity = (String) parameters.getOrDefault("severity", "info");
        @SuppressWarnings("unchecked")
        Map<String, Object> channelParams = (Map<String, Object>) parameters.getOrDefault("parameters", Map.of());
        
        return switch (channel) {
            case "slack" -> sendSlackNotification(message, severity, channelParams);
            case "teams" -> sendTeamsNotification(message, severity, channelParams);
            case "email" -> sendEmailNotification(message, severity, channelParams);
            case "webhook" -> sendWebhookNotification(message, severity, channelParams);
            case "console" -> sendConsoleNotification(message, severity, channelParams);
            default -> Mono.just(MCPToolResult.error("Unknown notification channel: " + channel));
        };
    }
    
    private Mono<MCPToolResult> sendSlackNotification(String message, String severity, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            try {
                // Create Slack message payload
                Map<String, Object> slackPayload = createSlackPayload(message, severity, params);
                
                // In a real implementation, you would use Slack Web API or webhook
                // For demo, we'll simulate the notification
                String result = simulateNotification("Slack", slackPayload);
                
                Map<String, Object> metadata = Map.of(
                    "channel", "slack",
                    "severity", severity,
                    "channel_id", params.getOrDefault("channel_id", "general"),
                    "sent_at", System.currentTimeMillis()
                );
                
                return MCPToolResult.withMetadata(result, metadata);
                
            } catch (Exception e) {
                return MCPToolResult.error("Failed to send Slack notification: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> sendTeamsNotification(String message, String severity, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            try {
                // Create Teams message payload
                Map<String, Object> teamsPayload = createTeamsPayload(message, severity, params);
                
                String result = simulateNotification("Teams", teamsPayload);
                
                Map<String, Object> metadata = Map.of(
                    "channel", "teams",
                    "severity", severity,
                    "webhook_url", params.getOrDefault("webhook_url", "not_provided"),
                    "sent_at", System.currentTimeMillis()
                );
                
                return MCPToolResult.withMetadata(result, metadata);
                
            } catch (Exception e) {
                return MCPToolResult.error("Failed to send Teams notification: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> sendEmailNotification(String message, String severity, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            try {
                @SuppressWarnings("unchecked")
                List<String> recipients = (List<String>) params.getOrDefault("recipients", List.of());
                String title = (String) params.getOrDefault("title", "Code Review Notification");
                
                if (recipients.isEmpty()) {
                    return MCPToolResult.error("Email recipients required");
                }
                
                // Create email content
                String emailContent = createEmailContent(message, severity, title, params);
                
                String result = simulateNotification("Email", Map.of(
                    "recipients", recipients,
                    "title", title,
                    "content", emailContent
                ));
                
                Map<String, Object> metadata = Map.of(
                    "channel", "email",
                    "severity", severity,
                    "recipients", recipients,
                    "title", title,
                    "sent_at", System.currentTimeMillis()
                );
                
                return MCPToolResult.withMetadata(result, metadata);
                
            } catch (Exception e) {
                return MCPToolResult.error("Failed to send email notification: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> sendWebhookNotification(String message, String severity, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            try {
                String webhookUrl = (String) params.get("webhook_url");
                if (webhookUrl == null || webhookUrl.isEmpty()) {
                    return MCPToolResult.error("Webhook URL required");
                }
                
                Map<String, Object> webhookPayload = Map.of(
                    "message", message,
                    "severity", severity,
                    "timestamp", System.currentTimeMillis(),
                    "source", "ai-code-review",
                    "additional_data", params
                );
                
                String result = simulateNotification("Webhook", webhookPayload);
                
                Map<String, Object> metadata = Map.of(
                    "channel", "webhook",
                    "severity", severity,
                    "webhook_url", webhookUrl,
                    "sent_at", System.currentTimeMillis()
                );
                
                return MCPToolResult.withMetadata(result, metadata);
                
            } catch (Exception e) {
                return MCPToolResult.error("Failed to send webhook notification: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> sendConsoleNotification(String message, String severity, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            String prefix = switch (severity) {
                case "critical" -> "🔴 CRITICAL";
                case "error" -> "🟠 ERROR";
                case "warning" -> "🟡 WARNING";
                default -> "🔵 INFO";
            };
            
            String formattedMessage = String.format("[%s] %s: %s", 
                java.time.LocalDateTime.now(), prefix, message);
            
            // Log to console/logger
            System.out.println(formattedMessage);
            
            Map<String, Object> metadata = Map.of(
                "channel", "console",
                "severity", severity,
                "logged_at", System.currentTimeMillis()
            );
            
            return MCPToolResult.withMetadata("Notification logged to console", metadata);
        });
    }
    
    private Map<String, Object> createSlackPayload(String message, String severity, Map<String, Object> params) {
        String color = switch (severity) {
            case "critical" -> "#ff0000";
            case "error" -> "#ff6600";
            case "warning" -> "#ffcc00";
            default -> "#36a64f";
        };
        
        Map<String, Object> attachment = Map.of(
            "color", color,
            "title", params.getOrDefault("title", "Code Review Update"),
            "text", message,
            "footer", "AI Code Review System",
            "ts", System.currentTimeMillis() / 1000
        );
        
        return Map.of(
            "channel", params.getOrDefault("channel_id", "general"),
            "attachments", List.of(attachment)
        );
    }
    
    private Map<String, Object> createTeamsPayload(String message, String severity, Map<String, Object> params) {
        String themeColor = switch (severity) {
            case "critical" -> "FF0000";
            case "error" -> "FF6600";
            case "warning" -> "FFCC00";
            default -> "36A64F";
        };
        
        return Map.of(
            "@type", "MessageCard",
            "@context", "https://schema.org/extensions",
            "summary", params.getOrDefault("title", "Code Review Update"),
            "themeColor", themeColor,
            "sections", List.of(Map.of(
                "activityTitle", "AI Code Review System",
                "activitySubtitle", "Severity: " + severity.toUpperCase(),
                "text", message
            ))
        );
    }
    
    private String createEmailContent(String message, String severity, String title, Map<String, Object> params) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append("<h2>").append(title).append("</h2>");
        html.append("<p><strong>Severity:</strong> ").append(severity.toUpperCase()).append("</p>");
        html.append("<p>").append(message).append("</p>");
        
        if (params.containsKey("pull_request_id")) {
            html.append("<p><strong>Pull Request ID:</strong> ").append(params.get("pull_request_id")).append("</p>");
        }
        
        html.append("<hr>");
        html.append("<p><em>This notification was generated automatically by the AI Code Review System.</em></p>");
        html.append("</body></html>");
        
        return html.toString();
    }
    
    private String simulateNotification(String channel, Object payload) {
        // In a real implementation, this would actually send the notification
        // For demo purposes, we'll just return a success message
        return String.format("Notification sent via %s. Payload: %s", channel, payload.toString());
    }
    
    @Override
    public String[] getRequiredCapabilities() {
        return new String[]{"network:send", "notification:send"};
    }
    
    @Override
    public boolean isAvailable() {
        // In a real implementation, you might check if notification services are configured
        return true;
    }
}