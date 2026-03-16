package harshal.temkar.ai.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadResponse {
	
    private Long documentId;
    private String filename;
    private Integer chunksCreated;
    private String status;
    private String message;
}