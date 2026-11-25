package net.cracksofdoom.parser.api;

import jakarta.validation.Valid;
import net.cracksofdoom.parser.application.IsoMessageService;
import net.cracksofdoom.parser.core.IsoMessageParser;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/messages")
public class IsoController {
    private final IsoMessageService service;

    public IsoController(IsoMessageService service) {
        this.service = service;
    }

    @PostMapping("/parse")
    public IsoMessageParser.Result parse(@Valid @RequestBody ParseRequest request) {
        return service.parse(request);
    }

    @PostMapping("/compare")
    public IsoMessageService.Comparison compare(@Valid @RequestBody CompareRequest request) {
        return service.compare(request);
    }
}
