package com.meishan.agri.rag.retrieval;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class KeywordRetriever {
    private static final Pattern TOKEN = Pattern.compile("[\\u4e00-\\u9fa5]{1}|[a-zA-Z0-9]+");

    public record Scored(int index, double score) {}

    public List<Scored> score(String query, List<String> chunks) {
        List<String> qTokens = tokenize(query);
        Map<String, Integer> df = new HashMap<>();
        for (String chunk : chunks) {
            Set<String> tokens = new HashSet<>(tokenize(chunk));
            for (String t : tokens) df.merge(t, 1, Integer::sum);
        }
        int n = Math.max(chunks.size(), 1);
        List<Scored> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            List<String> cTokens = tokenize(chunks.get(i));
            Map<String, Integer> tf = new HashMap<>();
            for (String t : cTokens) tf.merge(t, 1, Integer::sum);
            double score = 0;
            for (String t : qTokens) {
                double idf = Math.log(1 + (double) n / (1 + df.getOrDefault(t, 0)));
                score += tf.getOrDefault(t, 0) * idf;
            }
            result.add(new Scored(i, score));
        }
        result.sort((x, y) -> Double.compare(y.score(), x.score()));
        return result;
    }

    private List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        var m = TOKEN.matcher(text == null ? "" : text);
        while (m.find()) out.add(m.group());
        return out;
    }
}
