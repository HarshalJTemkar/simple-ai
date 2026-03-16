package harshal.temkar.ai.service.rag;

import java.util.List;

public interface IEmbeddingService {
    
	float[] embed(String text);
    
	List<float[]> embedBatch(List<String> texts);
}