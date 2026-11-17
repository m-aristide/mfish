package com.mfish.erp.parametre;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parametres")
public class ParametreController {

    private final ParametreService parametreService;

    public ParametreController(final ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @GetMapping
    public List<ParametreDto> lister() {
        return parametreService.lister();
    }

    @GetMapping("/{cle}")
    public ParametreDto trouver(@PathVariable final String cle) {
        return parametreService.trouver(cle);
    }

    @PutMapping("/{cle}")
    public ParametreDto enregistrer(@PathVariable final String cle,
                                    @Valid @RequestBody final ParametreRequest request) {
        return parametreService.enregistrer(cle, request.valeur());
    }

    /**
     * Corps de mise a jour d'un parametre.
     */
    public record ParametreRequest(@NotBlank String valeur) {
    }
}
