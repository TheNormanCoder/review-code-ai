package com.reviewcode.ai.mcp;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

/**
 * Filesystem operations tool for MCP
 * Provides safe filesystem access for AI models
 */
@Component
public class FileSystemTool implements MCPTool {
    
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".java", ".js", ".ts", ".py", ".md", ".yml", ".yaml", 
        ".json", ".xml", ".properties", ".sql", ".sh", ".bat"
    );
    
    private static final long MAX_FILE_SIZE = 1024 * 1024; // 1MB
    
    @Override
    public String getName() {
        return "filesystem";
    }
    
    @Override
    public String getDescription() {
        return "Read files, list directories, and analyze file structures. Limited to code and configuration files under 1MB.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
            "operation", Map.of(
                "type", "string",
                "description", "Filesystem operation to perform",
                "enum", List.of("read_file", "list_directory", "find_files", "analyze_structure", "get_file_info")
            ),
            "path", Map.of(
                "type", "string",
                "description", "File or directory path"
            ),
            "parameters", Map.of(
                "type", "object",
                "description", "Operation-specific parameters",
                "properties", Map.of(
                    "pattern", Map.of("type", "string", "description", "File pattern to match"),
                    "recursive", Map.of("type", "boolean", "description", "Search recursively"),
                    "max_depth", Map.of("type", "integer", "description", "Maximum directory depth"),
                    "include_hidden", Map.of("type", "boolean", "description", "Include hidden files")
                )
            )
        ));
        schema.put("required", List.of("operation", "path"));
        return schema;
    }
    
    @Override
    public Mono<MCPToolResult> execute(Map<String, Object> parameters) {
        String operation = (String) parameters.get("operation");
        String path = (String) parameters.get("path");
        @SuppressWarnings("unchecked")
        Map<String, Object> opParams = (Map<String, Object>) parameters.getOrDefault("parameters", Map.of());
        
        return switch (operation) {
            case "read_file" -> readFile(path);
            case "list_directory" -> listDirectory(path, opParams);
            case "find_files" -> findFiles(path, opParams);
            case "analyze_structure" -> analyzeStructure(path, opParams);
            case "get_file_info" -> getFileInfo(path);
            default -> Mono.just(MCPToolResult.error("Unknown operation: " + operation));
        };
    }
    
    private Mono<MCPToolResult> readFile(String filePath) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(filePath).normalize();
            
            if (!isAllowedFile(path)) {
                return MCPToolResult.error("File type not allowed or file too large: " + filePath);
            }
            
            try {
                String content = Files.readString(path);
                
                Map<String, Object> metadata = Map.of(
                    "file", filePath,
                    "size", content.length(),
                    "lines", content.split("\n").length,
                    "encoding", "UTF-8"
                );
                
                return MCPToolResult.withMetadata(content, metadata);
                
            } catch (IOException e) {
                return MCPToolResult.error("Failed to read file: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> listDirectory(String dirPath, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(dirPath).normalize();
            
            if (!Files.isDirectory(path)) {
                return MCPToolResult.error("Path is not a directory: " + dirPath);
            }
            
            boolean includeHidden = (Boolean) params.getOrDefault("include_hidden", false);
            
            try (Stream<Path> files = Files.list(path)) {
                List<Map<String, Object>> fileList = files
                    .filter(p -> includeHidden || !p.getFileName().toString().startsWith("."))
                    .map(this::pathToInfo)
                    .toList();
                
                Map<String, Object> metadata = Map.of(
                    "directory", dirPath,
                    "file_count", fileList.size(),
                    "include_hidden", includeHidden
                );
                
                return MCPToolResult.withMetadata(fileList, metadata);
                
            } catch (IOException e) {
                return MCPToolResult.error("Failed to list directory: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> findFiles(String rootPath, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(rootPath).normalize();
            String pattern = (String) params.getOrDefault("pattern", "*");
            boolean recursive = (Boolean) params.getOrDefault("recursive", true);
            int maxDepth = (Integer) params.getOrDefault("max_depth", 10);
            
            if (!Files.isDirectory(path)) {
                return MCPToolResult.error("Path is not a directory: " + rootPath);
            }
            
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                List<Map<String, Object>> foundFiles = new ArrayList<>();
                
                Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), maxDepth, 
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (matcher.matches(file.getFileName()) && isAllowedFile(file)) {
                                foundFiles.add(pathToInfo(file));
                            }
                            return FileVisitResult.CONTINUE;
                        }
                        
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            if (!recursive && !dir.equals(path)) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                
                Map<String, Object> metadata = Map.of(
                    "root_path", rootPath,
                    "pattern", pattern,
                    "found_files", foundFiles.size(),
                    "recursive", recursive
                );
                
                return MCPToolResult.withMetadata(foundFiles, metadata);
                
            } catch (IOException e) {
                return MCPToolResult.error("Failed to find files: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> analyzeStructure(String rootPath, Map<String, Object> params) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(rootPath).normalize();
            int maxDepth = (Integer) params.getOrDefault("max_depth", 5);
            
            try {
                Map<String, Object> structure = new HashMap<>();
                Map<String, Integer> extensionCounts = new HashMap<>();
                final int[] totalFiles = {0};
                final int[] totalDirs = {0};
                final long[] totalSize = {0};
                
                Files.walkFileTree(path, EnumSet.noneOf(FileVisitOption.class), maxDepth,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            totalFiles[0]++;
                            totalSize[0] += attrs.size();
                            
                            String fileName = file.getFileName().toString();
                            int lastDot = fileName.lastIndexOf('.');
                            if (lastDot > 0) {
                                String extension = fileName.substring(lastDot);
                                extensionCounts.merge(extension, 1, Integer::sum);
                            }
                            
                            return FileVisitResult.CONTINUE;
                        }
                        
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                            totalDirs[0]++;
                            return FileVisitResult.CONTINUE;
                        }
                    });
                
                structure.put("total_files", totalFiles[0]);
                structure.put("total_directories", totalDirs[0]);
                structure.put("total_size_bytes", totalSize[0]);
                structure.put("extension_counts", extensionCounts);
                
                Map<String, Object> metadata = Map.of(
                    "root_path", rootPath,
                    "max_depth", maxDepth,
                    "analysis_time", System.currentTimeMillis()
                );
                
                return MCPToolResult.withMetadata(structure, metadata);
                
            } catch (IOException e) {
                return MCPToolResult.error("Failed to analyze structure: " + e.getMessage());
            }
        });
    }
    
    private Mono<MCPToolResult> getFileInfo(String filePath) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(filePath).normalize();
            
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                
                Map<String, Object> info = Map.of(
                    "path", filePath,
                    "size", attrs.size(),
                    "is_directory", attrs.isDirectory(),
                    "is_regular_file", attrs.isRegularFile(),
                    "created_time", attrs.creationTime().toString(),
                    "modified_time", attrs.lastModifiedTime().toString(),
                    "readable", Files.isReadable(path),
                    "writable", Files.isWritable(path),
                    "executable", Files.isExecutable(path)
                );
                
                return MCPToolResult.success(info);
                
            } catch (IOException e) {
                return MCPToolResult.error("Failed to get file info: " + e.getMessage());
            }
        });
    }
    
    private boolean isAllowedFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        
        try {
            if (Files.size(path) > MAX_FILE_SIZE) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        
        String fileName = path.getFileName().toString().toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
    
    private Map<String, Object> pathToInfo(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return Map.of(
                "name", path.getFileName().toString(),
                "path", path.toString(),
                "type", attrs.isDirectory() ? "directory" : "file",
                "size", attrs.size(),
                "modified", attrs.lastModifiedTime().toString()
            );
        } catch (IOException e) {
            return Map.of(
                "name", path.getFileName().toString(),
                "path", path.toString(),
                "error", "Failed to read attributes"
            );
        }
    }
    
    @Override
    public String[] getRequiredCapabilities() {
        return new String[]{"filesystem:read"};
    }
}