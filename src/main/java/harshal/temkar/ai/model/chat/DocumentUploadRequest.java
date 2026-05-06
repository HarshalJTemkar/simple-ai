package harshal.temkar.ai.model.chat;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentUploadRequest {
	
    private MultipartFile file;
    private String metadata;
}