package net.cracksofdoom.parser.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import net.cracksofdoom.parser.core.IsoMessageParser;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({ IsoMessageParser.ParseException.class, IllegalArgumentException.class })
    ResponseEntity<Map<String, Object>> badRequest(Exception exception) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage());
        if (exception instanceof IsoMessageParser.ParseException parse)
            body.put("offset", parse.getOffset());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request", "field",
                exception.getBindingResult().getFieldErrors().get(0).getField()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> unreadable() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Malformed JSON request"));
    }
}
