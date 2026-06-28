package com.distrollm.classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Inline comment on thread safety:
// ComplexityClassifier is designed to be completely stateless. It does not hold 
// any mutable instance variables or shared state. All scoring calculations and 
// evaluations are performed using local variables confined to the executing thread's 
// own method stack frame. 
// This stateless nature makes the class intrinsically thread-safe. Multiple 
// threads can call classify() concurrently without acquiring locks, avoiding 
// contention, bottlenecks, and the possibility of race conditions entirely.
public class ComplexityClassifier {
    
    private static final String[] KEYWORDS = {
        "explain", "compare", "analyze", "design", "implement",
        "architecture", "difference between", "how does", "why does"
    };

    public QueryComplexity classify(String prompt) {
        Map<String, Object> result = classifyWithReason(prompt);
        return (QueryComplexity) result.get("complexity");
    }

    public Map<String, Object> classifyWithReason(String prompt) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        
        if (prompt == null) {
            prompt = "";
        }

        // Rule 1: Length
        if (prompt.length() > 500) {
            score += 3;
            reasons.add("Length > 500 characters (+3)");
        } else if (prompt.length() > 150) {
            score += 1;
            reasons.add("Length > 150 characters (+1)");
        }
        
        // Rule 2: Keywords
        String lowerPrompt = prompt.toLowerCase();
        int keywordMatches = 0;
        for (String kw : KEYWORDS) {
            if (lowerPrompt.contains(kw)) {
                keywordMatches++;
                reasons.add("Contains keyword '" + kw + "' (+2)");
                if (keywordMatches == 3) {
                    break; // Cap at max +6 points
                }
            }
        }
        score += keywordMatches * 2;

        // Rule 3: Code snippets
        if (prompt.contains("```") || prompt.contains("def ") || prompt.contains("public class ")) {
            score += 3;
            reasons.add("Contains code snippets (+3)");
        }

        // Rule 4: Multiple question marks
        long questionCount = prompt.chars().filter(ch -> ch == '?').count();
        if (questionCount > 2) {
            score += 2;
            reasons.add("Contains multiple '?' characters (+2)");
        }

        // Rule 5: Word count
        String[] words = prompt.trim().split("\\s+");
        if (words.length > 100) {
            score += 2;
            reasons.add("Word count > 100 (+2)");
        }

        // Determine complexity
        QueryComplexity complexity;
        if (score <= 3) {
            complexity = QueryComplexity.SIMPLE;
        } else if (score <= 7) {
            complexity = QueryComplexity.MEDIUM;
        } else {
            complexity = QueryComplexity.COMPLEX;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("complexity", complexity);
        result.put("score", score);
        result.put("reasons", reasons);
        
        return result;
    }
}
