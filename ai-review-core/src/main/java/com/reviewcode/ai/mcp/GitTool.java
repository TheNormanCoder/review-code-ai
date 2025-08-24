package com.reviewcode.ai.mcp;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Git operations tool for MCP
 * Provides direct Git repository access for AI models
 */
@Component
public class GitTool implements MCPTool {
    
    @Override
    public String getName() {
        return "git";
    }
    
    @Override
    public String getDescription() {
        return "Execute Git commands and analyze repository data. Can get diffs, file contents, commit history, and branch information.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "command", Map.of(
                "type", "string",
                "description", "Git command to execute",
                "enum", List.of("diff", "log", "show", "status", "blame", "file-content")
            ),
            "repository", Map.of(
                "type", "string",
                "description", "Repository path or URL"
            ),
            "parameters", Map.of(
                "type", "object",
                "description", "Command-specific parameters",
                "properties", Map.of(
                    "file", Map.of("type", "string", "description", "File path"),
                    "commit", Map.of("type", "string", "description", "Commit hash"),
                    "branch", Map.of("type", "string", "description", "Branch name"),
                    "since", Map.of("type", "string", "description", "Date since"),
                    "author", Map.of("type", "string", "description", "Author filter")
                )
            )
        ));
        schema.put("required", List.of("command", "repository"));
        return schema;
    }
    
    @Override
    public Mono<MCPToolResult> execute(Map<String, Object> parameters) {
        String command = (String) parameters.get("command");
        String repository = (String) parameters.get("repository");
        @SuppressWarnings("unchecked")
        Map<String, Object> cmdParams = (Map<String, Object>) parameters.getOrDefault("parameters", Map.of());
        
        return switch (command) {
            case "diff" -> executeDiff(repository, cmdParams);
            case "log" -> executeLog(repository, cmdParams);
            case "show" -> executeShow(repository, cmdParams);
            case "status" -> executeStatus(repository);
            case "blame" -> executeBlame(repository, cmdParams);
            case "file-content" -> getFileContent(repository, cmdParams);
            default -> Mono.just(MCPToolResult.error("Unknown git command: " + command));
        };
    }
    
    private Mono<MCPToolResult> executeDiff(String repository, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            StringBuilder cmd = new StringBuilder("git -C ").append(repository).append(" diff");
            
            if (params.containsKey("commit")) {
                cmd.append(" ").append(params.get("commit"));
            }
            if (params.containsKey("file")) {
                cmd.append(" -- ").append(params.get("file"));
            }
            
            String result = executeGitCommand(cmd.toString());
            
            Map<String, Object> metadata = Map.of(
                "command", "diff",
                "repository", repository,
                "lines", result.split("\n").length
            );
            
            return MCPToolResult.withMetadata(result, metadata);
        }).onErrorReturn(MCPToolResult.error("Failed to execute git diff"));
    }
    
    private Mono<MCPToolResult> executeLog(String repository, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            StringBuilder cmd = new StringBuilder("git -C ").append(repository)
                    .append(" log --oneline --max-count=50");
            
            if (params.containsKey("author")) {
                cmd.append(" --author=\"").append(params.get("author")).append("\"");
            }
            if (params.containsKey("since")) {
                cmd.append(" --since=\"").append(params.get("since")).append("\"");
            }
            if (params.containsKey("file")) {
                cmd.append(" -- ").append(params.get("file"));
            }
            
            String result = executeGitCommand(cmd.toString());
            String[] commits = result.split("\n");
            
            Map<String, Object> metadata = Map.of(
                "command", "log",
                "repository", repository,
                "commit_count", commits.length
            );
            
            return MCPToolResult.withMetadata(List.of(commits), metadata);
        }).onErrorReturn(MCPToolResult.error("Failed to execute git log"));
    }
    
    private Mono<MCPToolResult> executeShow(String repository, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            if (!params.containsKey("commit")) {
                return MCPToolResult.error("Commit hash required for git show");
            }
            
            String cmd = "git -C " + repository + " show " + params.get("commit");
            String result = executeGitCommand(cmd);
            
            Map<String, Object> metadata = Map.of(
                "command", "show",
                "commit", params.get("commit"),
                "repository", repository
            );
            
            return MCPToolResult.withMetadata(result, metadata);
        }).onErrorReturn(MCPToolResult.error("Failed to execute git show"));
    }
    
    private Mono<MCPToolResult> executeStatus(String repository) {
        return Mono.fromCallable(() -> {
            String cmd = "git -C " + repository + " status --porcelain";
            String result = executeGitCommand(cmd);
            
            String[] files = result.isEmpty() ? new String[0] : result.split("\n");
            
            Map<String, Object> metadata = Map.of(
                "command", "status",
                "repository", repository,
                "changed_files", files.length
            );
            
            return MCPToolResult.withMetadata(List.of(files), metadata);
        }).onErrorReturn(MCPToolResult.error("Failed to execute git status"));
    }
    
    private Mono<MCPToolResult> executeBlame(String repository, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            if (!params.containsKey("file")) {
                return MCPToolResult.error("File path required for git blame");
            }
            
            String cmd = "git -C " + repository + " blame " + params.get("file");
            String result = executeGitCommand(cmd);
            
            Map<String, Object> metadata = Map.of(
                "command", "blame",
                "file", params.get("file"),
                "repository", repository
            );
            
            return MCPToolResult.withMetadata(result, metadata);
        }).onErrorReturn(MCPToolResult.error("Failed to execute git blame"));
    }
    
    private Mono<MCPToolResult> getFileContent(String repository, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            if (!params.containsKey("file")) {
                return MCPToolResult.error("File path required");
            }
            
            String filePath = repository + "/" + params.get("file");
            try {
                String content = java.nio.file.Files.readString(java.nio.file.Paths.get(filePath));
                
                Map<String, Object> metadata = Map.of(
                    "file", params.get("file"),
                    "repository", repository,
                    "size", content.length(),
                    "lines", content.split("\n").length
                );
                
                return MCPToolResult.withMetadata(content, metadata);
            } catch (Exception e) {
                return MCPToolResult.error("Failed to read file: " + e.getMessage());
            }
        });
    }
    
    private String executeGitCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            process.waitFor();
            return output.toString().trim();
            
        } catch (Exception e) {
            throw new RuntimeException("Git command failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String[] getRequiredCapabilities() {
        return new String[]{"filesystem:read", "process:execute"};
    }
}