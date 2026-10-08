package ru.yandex.practicum.mymarket.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex, Model model, HttpServletRequest request) {
        log.warn("404 {} -> {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 404);
        model.addAttribute("message", ex.getMessage());
        return "404";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalState(IllegalStateException ex, Model model, HttpServletRequest request) {
        log.warn("400 {} -> {}", request.getRequestURI(), ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("message", ex.getMessage());
        return "400";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("400 illegal argument: {}", ex.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("message", ex.getMessage());
        return "400";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAll(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        model.addAttribute("status", 500);
        model.addAttribute("message", "Внутренняя ошибка сервера");
        return "500";
    }
}
