package harshal.temkar.ai.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class DocumentParser {

    private final Tika tika = new Tika();

    public String parseDocument(MultipartFile file) throws IOException {
    	
        log.debug("Parsing document: {}, type: {}", file.getOriginalFilename(), file.getContentType());
        
        try {
            String content = tika.parseToString(file.getInputStream());
            log.debug("Extracted {} characters from document", content.length());
            return content;
        } catch (Exception e) {
            log.error("Failed to parse document: {}", file.getOriginalFilename(), e);
            throw new IOException("Failed to parse document: " + e.getMessage(), e);
        }
    }
}