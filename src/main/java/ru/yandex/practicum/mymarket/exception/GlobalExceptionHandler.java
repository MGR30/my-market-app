package ru.yandex.practicum.mymarket.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.MissingRequestValueException;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public Mono<ResponseEntity<Void>> handleNoResource(NoResourceFoundException ex) {
        return Mono.just(ResponseEntity.notFound().build());
    }


    @ExceptionHandler(MissingRequestValueException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Rendering> handleMissingValue(MissingRequestValueException ex) {
        log.warn("400 missing value: {}", ex.getMessage());
        return renderError(400, "400", ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<Rendering> handleNotFound(NoSuchElementException ex, ServerHttpRequest request) {
        log.warn("404 {} -> {}", request.getPath(), ex.getMessage());
        return renderError(404, "404", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Rendering> handleIllegalState(IllegalStateException ex, ServerHttpRequest request) {
        log.warn("400 {} -> {}", request.getPath(), ex.getMessage());
        return renderError(400, "400", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Rendering> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("400 illegal argument: {}", ex.getMessage());
        return renderError(400, "400", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<String>> handleAll(Exception ex) {
        log.error("Unexpected error", ex);
        return Mono.just(ResponseEntity.status(500).body("Внутренняя ошибка сервера"));
    }

    private Mono<Rendering> renderError(int status, String view, String message) {
        return Mono.just(Rendering.view(view)
                .modelAttribute("status", status)
                .modelAttribute("message", message)
                .build());
    }
}