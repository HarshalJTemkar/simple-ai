package harshal.temkar.ai.service.rag;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements IEmbeddingService {

    private final EmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        log.debug("Generating embedding for text of length: {}", text.length());
        
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            
            float[] embedding = response.getResults().get(0).getOutput();
            
            log.debug("Generated embedding with dimension: {}", embedding.length);
            return embedding;
            
        } catch (Exception e) {
            log.error("Failed to generate embedding for text", e);
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        log.debug("Generating embeddings for {} texts", texts.size());
        
        try {
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            
            List<float[]> embeddings = response.getResults().stream()
                    .map(result -> result.getOutput())
                    .collect(Collectors.toList());
            
            log.debug("Generated {} embeddings", embeddings.size());
            return embeddings;
            
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings", e);
            throw new RuntimeException("Batch embedding generation failed: " + e.getMessage(), e);
        }
    }
}