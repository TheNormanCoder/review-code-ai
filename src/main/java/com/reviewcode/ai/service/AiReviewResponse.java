package com.reviewcode.ai.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiReviewResponse {
    
    private String decision;
    private String summary;
    private Integer score;
    private List<Finding> findings;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Finding {
        private String fileName;
        private Integer lineNumber;
        private String type;
        private String severity;
        private String description;
        private String suggestion;
        private String codeSnippet;
        private String proposedCode;
        private String ruleId;
    }
}