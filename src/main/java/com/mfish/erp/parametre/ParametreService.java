package com.mfish.erp.parametre;

import com.mfish.erp.commun.NotFoundException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ParametreService {

    private final ParametreRepository parametreRepository;

    public ParametreService(final ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    public List<ParametreDto> lister() {
        return parametreRepository.findAll().stream().map(ParametreDto::from).toList();
    }

    public ParametreDto trouver(final String cle) {
        return parametreRepository.findByCle(cle).map(ParametreDto::from)
                .orElseThrow(() -> new NotFoundException("Parametre introuvable : " + cle));
    }

    public ParametreDto enregistrer(final String cle, final String valeur) {
        final Parametre parametre = parametreRepository.findByCle(cle)
                .orElseGet(() -> new Parametre(cle, valeur));
        parametre.setValeur(valeur);
        return ParametreDto.from(parametreRepository.save(parametre));
    }

    public Map<String, String> toutesLesValeurs() {
        final Map<String, String> valeurs = new java.util.LinkedHashMap<>();
        parametreRepository.findAll().forEach(p -> valeurs.put(p.getCle(), p.getValeur()));
        return valeurs;
    }
}
