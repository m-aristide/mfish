package com.mfish.erp.quartier;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quartiers")
public class QuartierController {

    private final QuartierService quartierService;

    public QuartierController(final QuartierService quartierService) {
        this.quartierService = quartierService;
    }

    @GetMapping
    public List<QuartierDto> lister() {
        return quartierService.lister();
    }

    @GetMapping("/{id}")
    public QuartierDto trouver(@PathVariable final UUID id) {
        return quartierService.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuartierDto creer(@Valid @RequestBody final QuartierRequest request) {
        return quartierService.creer(request);
    }

    @PutMapping("/{id}")
    public QuartierDto modifier(@PathVariable final UUID id, @Valid @RequestBody final QuartierRequest request) {
        return quartierService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable final UUID id) {
        quartierService.supprimer(id);
    }
}
