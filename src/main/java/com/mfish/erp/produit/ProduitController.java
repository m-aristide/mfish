package com.mfish.erp.produit;

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
@RequestMapping("/api/produits")
public class ProduitController {

    private final ProduitService produitService;

    public ProduitController(final ProduitService produitService) {
        this.produitService = produitService;
    }

    @GetMapping
    public List<ProduitDto> lister() {
        return produitService.lister();
    }

    @GetMapping("/{id}")
    public ProduitDto trouver(@PathVariable final UUID id) {
        return produitService.trouver(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProduitDto creer(@Valid @RequestBody final ProduitRequest request) {
        return produitService.creer(request);
    }

    @PutMapping("/{id}")
    public ProduitDto modifier(@PathVariable final UUID id, @Valid @RequestBody final ProduitRequest request) {
        return produitService.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable final UUID id) {
        produitService.supprimer(id);
    }
}
