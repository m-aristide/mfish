package com.mfish.erp.client;

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
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(final ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<ClientDto> lister() {
        return clientService.lister();
    }

    @GetMapping("/{id}")
    public ClientDto trouver(@PathVariable final UUID id) {
        return clientService.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDto creer(@Valid @RequestBody final ClientRequest request) {
        return clientService.creer(request);
    }

    @PutMapping("/{id}")
    public ClientDto modifier(@PathVariable final UUID id, @Valid @RequestBody final ClientRequest request) {
        return clientService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable final UUID id) {
        clientService.supprimer(id);
    }
}
