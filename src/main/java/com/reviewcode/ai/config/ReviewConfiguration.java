package com.reviewcode.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "review")
public class ReviewConfiguration {
    
    private Thresholds thresholds = new Thresholds();
    private Rules rules = new Rules();
    private Patterns patterns = new Patterns();
    private Teams teams = new Teams();

    public static class Thresholds {
        private int autoApproveScore = 80;
        private int autoRejectScore = 30;
        private int maxMethodLength = 25;
        private int maxClassLength = 300;
        private int maxParameters = 5;
        private int criticalFindingsThreshold = 0;
        private int highFindingsThreshold = 3;
        
        // Getters and setters
        public int getAutoApproveScore() { return autoApproveScore; }
        public void setAutoApproveScore(int autoApproveScore) { this.autoApproveScore = autoApproveScore; }
        
        public int getAutoRejectScore() { return autoRejectScore; }
        public void setAutoRejectScore(int autoRejectScore) { this.autoRejectScore = autoRejectScore; }
        
        public int getMaxMethodLength() { return maxMethodLength; }
        public void setMaxMethodLength(int maxMethodLength) { this.maxMethodLength = maxMethodLength; }
        
        public int getMaxClassLength() { return maxClassLength; }
        public void setMaxClassLength(int maxClassLength) { this.maxClassLength = maxClassLength; }
        
        public int getMaxParameters() { return maxParameters; }
        public void setMaxParameters(int maxParameters) { this.maxParameters = maxParameters; }
        
        public int getCriticalFindingsThreshold() { return criticalFindingsThreshold; }
        public void setCriticalFindingsThreshold(int criticalFindingsThreshold) { this.criticalFindingsThreshold = criticalFindingsThreshold; }
        
        public int getHighFindingsThreshold() { return highFindingsThreshold; }
        public void setHighFindingsThreshold(int highFindingsThreshold) { this.highFindingsThreshold = highFindingsThreshold; }
    }

    public static class Rules {
        private List<String> disabled = List.of();
        private List<String> enabled = List.of();
        private Map<String, String> severity = Map.of();
        private boolean enableCleanCode = true;
        private boolean enableSolid = true;
        private boolean enableDdd = true;
        private boolean enableSecurity = true;
        private boolean enablePerformance = true;
        
        // Getters and setters
        public List<String> getDisabled() { return disabled; }
        public void setDisabled(List<String> disabled) { this.disabled = disabled; }
        
        public List<String> getEnabled() { return enabled; }
        public void setEnabled(List<String> enabled) { this.enabled = enabled; }
        
        public Map<String, String> getSeverity() { return severity; }
        public void setSeverity(Map<String, String> severity) { this.severity = severity; }
        
        public boolean isEnableCleanCode() { return enableCleanCode; }
        public void setEnableCleanCode(boolean enableCleanCode) { this.enableCleanCode = enableCleanCode; }
        
        public boolean isEnableSolid() { return enableSolid; }
        public void setEnableSolid(boolean enableSolid) { this.enableSolid = enableSolid; }
        
        public boolean isEnableDdd() { return enableDdd; }
        public void setEnableDdd(boolean enableDdd) { this.enableDdd = enableDdd; }
        
        public boolean isEnableSecurity() { return enableSecurity; }
        public void setEnableSecurity(boolean enableSecurity) { this.enableSecurity = enableSecurity; }
        
        public boolean isEnablePerformance() { return enablePerformance; }
        public void setEnablePerformance(boolean enablePerformance) { this.enablePerformance = enablePerformance; }
    }

    public static class Patterns {
        private List<String> ignoreFiles = List.of("*.test.js", "*Test.java", "*.spec.ts");
        private List<String> criticalFiles = List.of("*Security*.java", "*Auth*.java", "*Payment*.java");
        private Map<String, List<String>> customPatterns = Map.of();
        private Whitelist whitelist = new Whitelist();
        
        public static class Whitelist {
            private List<String> magicNumbers = List.of("0", "1", "-1", "100");
            private List<String> allowedSecrets = List.of("test", "localhost", "example");
            private List<String> skipSecurityChecks = List.of();
            
            // Getters and setters
            public List<String> getMagicNumbers() { return magicNumbers; }
            public void setMagicNumbers(List<String> magicNumbers) { this.magicNumbers = magicNumbers; }
            
            public List<String> getAllowedSecrets() { return allowedSecrets; }
            public void setAllowedSecrets(List<String> allowedSecrets) { this.allowedSecrets = allowedSecrets; }
            
            public List<String> getSkipSecurityChecks() { return skipSecurityChecks; }
            public void setSkipSecurityChecks(List<String> skipSecurityChecks) { this.skipSecurityChecks = skipSecurityChecks; }
        }
        
        // Getters and setters
        public List<String> getIgnoreFiles() { return ignoreFiles; }
        public void setIgnoreFiles(List<String> ignoreFiles) { this.ignoreFiles = ignoreFiles; }
        
        public List<String> getCriticalFiles() { return criticalFiles; }
        public void setCriticalFiles(List<String> criticalFiles) { this.criticalFiles = criticalFiles; }
        
        public Map<String, List<String>> getCustomPatterns() { return customPatterns; }
        public void setCustomPatterns(Map<String, List<String>> customPatterns) { this.customPatterns = customPatterns; }
        
        public Whitelist getWhitelist() { return whitelist; }
        public void setWhitelist(Whitelist whitelist) { this.whitelist = whitelist; }
    }

    public static class Teams {
        private Map<String, TeamConfig> teamConfigs = Map.of();
        
        public static class TeamConfig {
            private String name;
            private List<String> members = List.of();
            private Thresholds customThresholds;
            private List<String> additionalRules = List.of();
            private boolean strictMode = false;
            
            // Getters and setters
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            
            public List<String> getMembers() { return members; }
            public void setMembers(List<String> members) { this.members = members; }
            
            public Thresholds getCustomThresholds() { return customThresholds; }
            public void setCustomThresholds(Thresholds customThresholds) { this.customThresholds = customThresholds; }
            
            public List<String> getAdditionalRules() { return additionalRules; }
            public void setAdditionalRules(List<String> additionalRules) { this.additionalRules = additionalRules; }
            
            public boolean isStrictMode() { return strictMode; }
            public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }
        }
        
        // Getters and setters
        public Map<String, TeamConfig> getTeamConfigs() { return teamConfigs; }
        public void setTeamConfigs(Map<String, TeamConfig> teamConfigs) { this.teamConfigs = teamConfigs; }
    }

    // Main getters and setters
    public Thresholds getThresholds() { return thresholds; }
    public void setThresholds(Thresholds thresholds) { this.thresholds = thresholds; }
    
    public Rules getRules() { return rules; }
    public void setRules(Rules rules) { this.rules = rules; }
    
    public Patterns getPatterns() { return patterns; }
    public void setPatterns(Patterns patterns) { this.patterns = patterns; }
    
    public Teams getTeams() { return teams; }
    public void setTeams(Teams teams) { this.teams = teams; }
}