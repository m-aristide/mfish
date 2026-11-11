package com.mfish.erp.produit;

import com.mfish.erp.commun.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;

    public ProduitService(final ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    public List<ProduitDto> lister() {
        return produitRepository.findAll().stream().map(ProduitDto::from).toList();
    }

    public ProduitDto trouver(final UUID id) {
        return produitRepository.findById(id).map(ProduitDto::from)
                .orElseThrow(() -> new NotFoundException("Produit introuvable : " + id));
    }

    public ProduitDto creer(final ProduitRequest request) {
        final Produit produit = new Produit(request.nom(), request.prix(), request.unite(),
                request.actif(), request.image());
        return ProduitDto.from(produitRepository.save(produit));
    }

    public ProduitDto modifier(final UUID id, final ProduitRequest request) {
        final Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produit introuvable : " + id));
        produit.setNom(request.nom());
        produit.setPrix(request.prix());
        produit.setUnite(request.unite());
        produit.setActif(request.actif());
        produit.setImage(request.image());
        return ProduitDto.from(produitRepository.save(produit));
    }

    public void supprimer(final UUID id) {
        if (!produitRepository.existsById(id)) {
            throw new NotFoundException("Produit introuvable : " + id);
        }
        produitRepository.deleteById(id);
    }
}
