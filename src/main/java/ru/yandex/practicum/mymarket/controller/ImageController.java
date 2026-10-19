package ru.yandex.practicum.mymarket.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller
public class ImageController {

    @GetMapping("/images/{filename:.+}")
    public Mono<ResponseEntity<Resource>> image(@PathVariable String filename) {
        ClassPathResource resource = new ClassPathResource("static/images/" + filename);

        if (!resource.exists()) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource));
    }
}
