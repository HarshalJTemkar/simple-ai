package harshal.temkar.ai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import harshal.temkar.ai.config.RAGProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TextChunker {

    private final RAGProperties ragProperties;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s+");

    public List<String> chunkText(String text) {
        String strategy = ragProperties.getChunking().getStrategy();
        
        log.debug("Chunking text ({} chars) using strategy: {}", text.length(), strategy);
        
        switch (strategy.toLowerCase()) {
            case "sentence":
                return chunkBySentence(text);
            case "paragraph":
                return chunkByParagraph(text);
            default:
                return chunkByFixedSize(text);
        }
    }

    private List<String> chunkBySentence(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_PATTERN.split(text);
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkSize = ragProperties.getChunking().getChunkSize();
        int overlap = ragProperties.getChunking().getChunkOverlap();
        
        for (String sentence : sentences) {
            if (currentChunk.length() + sentence.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                
                // Add overlap
                String[] words = currentChunk.toString().split("\\s+");
                int overlapWords = Math.min(overlap / 5, words.length);
                currentChunk = new StringBuilder();
                for (int i = Math.max(0, words.length - overlapWords); i < words.length; i++) {
                    currentChunk.append(words[i]).append(" ");
                }
            }
            currentChunk.append(sentence).append(". ");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        log.debug("Created {} chunks using sentence strategy", chunks.size());
        return chunks;
    }

    private List<String> chunkByParagraph(String text) {
        String[] paragraphs = text.split("\\n\\n+");
        List<String> chunks = new ArrayList<>();
        
        for (String paragraph : paragraphs) {
            if (paragraph.trim().length() > 0) {
                chunks.add(paragraph.trim());
            }
        }
        
        log.debug("Created {} chunks using paragraph strategy", chunks.size());
        return chunks;
    }

    private List<String> chunkByFixedSize(String text) {
        List<String> chunks = new ArrayList<>();
        int chunkSize = ragProperties.getChunking().getChunkSize();
        int overlap = ragProperties.getChunking().getChunkOverlap();
        
        for (int i = 0; i < text.length(); i += (chunkSize - overlap)) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        
        log.debug("Created {} chunks using fixed-size strategy", chunks.size());
        return chunks;
    }
}