package com.reviewcode.ai.mcp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database query tool for MCP
 * Provides safe database access for AI models to query metrics and data
 */
@Component
public class DatabaseTool implements MCPTool {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    // Pre-defined safe queries to prevent SQL injection
    private static final Map<String, String> SAFE_QUERIES = Map.of(
        "pull_requests_summary", """
            SELECT status, COUNT(*) as count, AVG(review_score) as avg_score
            FROM pull_requests 
            WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
            GROUP BY status
            """,
        
        "recent_reviews", """
            SELECT pr.title, cr.review_type, cr.overall_score, cr.created_at
            FROM pull_requests pr 
            JOIN code_reviews cr ON pr.id = cr.pull_request_id
            WHERE cr.created_at > CURRENT_DATE - INTERVAL '7 days'
            ORDER BY cr.created_at DESC
            LIMIT 20
            """,
        
        "top_authors", """
            SELECT author, COUNT(*) as pr_count, AVG(review_score) as avg_score
            FROM pull_requests 
            WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
            GROUP BY author
            ORDER BY pr_count DESC
            LIMIT 10
            """,
        
        "security_findings", """
            SELECT rf.severity, rf.category, COUNT(*) as count
            FROM review_findings rf
            JOIN code_reviews cr ON rf.review_id = cr.id
            WHERE cr.created_at > CURRENT_DATE - INTERVAL '30 days'
            AND rf.category LIKE '%security%'
            GROUP BY rf.severity, rf.category
            ORDER BY count DESC
            """,
        
        "quality_trends", """
            SELECT DATE(created_at) as date, 
                   AVG(overall_score) as avg_score,
                   COUNT(*) as review_count
            FROM code_reviews
            WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
            GROUP BY DATE(created_at)
            ORDER BY date DESC
            """,
        
        "file_hotspots", """
            SELECT rf.file_path, COUNT(*) as issue_count, 
                   AVG(CASE WHEN rf.severity = 'CRITICAL' THEN 4 
                           WHEN rf.severity = 'HIGH' THEN 3
                           WHEN rf.severity = 'MEDIUM' THEN 2 
                           ELSE 1 END) as severity_score
            FROM review_findings rf
            JOIN code_reviews cr ON rf.review_id = cr.id
            WHERE cr.created_at > CURRENT_DATE - INTERVAL '30 days'
            GROUP BY rf.file_path
            HAVING COUNT(*) > 1
            ORDER BY issue_count DESC, severity_score DESC
            LIMIT 15
            """
    );
    
    @Override
    public String getName() {
        return "database";
    }
    
    @Override
    public String getDescription() {
        return "Query database for metrics, statistics, and historical data about code reviews, pull requests, and quality trends. Only predefined safe queries are allowed.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "query", Map.of(
                "type", "string",
                "description", "Predefined query name to execute",
                "enum", SAFE_QUERIES.keySet().stream().toList()
            ),
            "parameters", Map.of(
                "type", "object",
                "description", "Query parameters (if supported)",
                "properties", Map.of(
                    "limit", Map.of("type", "integer", "description", "Result limit"),
                    "days", Map.of("type", "integer", "description", "Number of days to look back"),
                    "author", Map.of("type", "string", "description", "Filter by author")
                )
            )
        ));
        schema.put("required", List.of("query"));
        return schema;
    }
    
    @Override
    public Mono<MCPToolResult> execute(Map<String, Object> parameters) {
        String queryName = (String) parameters.get("query");
        @SuppressWarnings("unchecked")
        Map<String, Object> queryParams = (Map<String, Object>) parameters.getOrDefault("parameters", Map.of());
        
        return Mono.fromCallable(() -> {
            String sql = SAFE_QUERIES.get(queryName);
            if (sql == null) {
                return MCPToolResult.error("Unknown query: " + queryName + ". Available queries: " + 
                    String.join(", ", SAFE_QUERIES.keySet()));
            }
            
            try {
                // Apply parameter modifications for supported queries
                sql = applyQueryParameters(sql, queryParams);
                
                List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
                
                Map<String, Object> metadata = Map.of(
                    "query", queryName,
                    "result_count", results.size(),
                    "execution_time", System.currentTimeMillis()
                );
                
                return MCPToolResult.withMetadata(results, metadata);
                
            } catch (Exception e) {
                return MCPToolResult.error("Database query failed: " + e.getMessage());
            }
        });
    }
    
    private String applyQueryParameters(String sql, Map<String, Object> params) {
        // Safe parameter substitution for supported queries
        if (params.containsKey("limit") && sql.contains("LIMIT")) {
            int limit = Math.min(100, (Integer) params.get("limit")); // Max 100 results
            sql = sql.replaceAll("LIMIT \\d+", "LIMIT " + limit);
        }
        
        if (params.containsKey("days")) {
            int days = Math.min(365, (Integer) params.get("days")); // Max 1 year
            sql = sql.replaceAll("INTERVAL '\\d+ days'", "INTERVAL '" + days + " days'");
        }
        
        if (params.containsKey("author")) {
            String author = ((String) params.get("author")).replaceAll("[';\"\\\\]", ""); // Sanitize
            if (sql.contains("WHERE")) {
                sql = sql.replace("WHERE", "WHERE author = '" + author + "' AND");
            }
        }
        
        return sql;
    }
    
    @Override
    public String[] getRequiredCapabilities() {
        return new String[]{"database:read"};
    }
    
    @Override
    public boolean isAvailable() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}