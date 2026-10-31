package com.mfish.erp.commande;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    private final CommandeApplicationService commandeApplicationService;

    public CommandeController(final CommandeApplicationService commandeApplicationService) {
        this.commandeApplicationService = commandeApplicationService;
    }

    @GetMapping
    public List<CommandeDto> lister() {
        return commandeApplicationService.lister();
    }

    @GetMapping("/{id}")
    public CommandeDto trouver(@PathVariable final UUID id) {
        return commandeApplicationService.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommandeDto creer(@Valid @RequestBody final CommandeRequest request) {
        return commandeApplicationService.creer(request);
    }

    @PatchMapping("/{id}/statut")
    public CommandeDto changerStatut(@PathVariable final UUID id,
                                     @Valid @RequestBody final StatutCommandeRequest request) {
        return commandeApplicationService.changerStatut(id, request.statut());
    }

    @PostMapping("/{id}/paiements")
    public CommandeDto enregistrerPaiement(@PathVariable final UUID id,
                                           @Valid @RequestBody final PaiementRequest request) {
        return commandeApplicationService.enregistrerPaiement(id, request.montant());
    }

    @PostMapping("/{id}/annulation")
    public CommandeDto annuler(@PathVariable final UUID id) {
        return commandeApplicationService.annuler(id);
    }
}
