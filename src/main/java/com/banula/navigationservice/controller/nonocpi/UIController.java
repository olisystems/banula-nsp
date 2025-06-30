package com.banula.navigationservice.controller.nonocpi;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLConnection;

@RestController
@RequestMapping("/navigator/ui")
@Slf4j
@AllArgsConstructor
public class UIController {

    private final ResourceLoader resourceLoader;

    @GetMapping("/assets/{fileName:.+}")
    public ResponseEntity<Resource> serveAsset(@PathVariable String fileName) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:/static/assets/" + fileName);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Guess the MIME type
        String contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE; // Fallback for unknown types
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }


    @RequestMapping(value = { "/**" })
    public Resource redirect() {
        return new ClassPathResource("/static/index.html");
    }
}
