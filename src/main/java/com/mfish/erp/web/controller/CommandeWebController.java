package com.mfish.erp.web.controller;

import com.mfish.erp.client.ClientDto;
import com.mfish.erp.client.ClientService;
import com.mfish.erp.commande.CommandeApplicationService;
import com.mfish.erp.commande.CommandeDto;
import com.mfish.erp.commande.CommandeInvalideException;
import com.mfish.erp.commande.CommandeRequest;
import com.mfish.erp.commande.LigneCommandeRequest;
import com.mfish.erp.commande.SourceCommande;
import com.mfish.erp.commande.StatutCommande;
import com.mfish.erp.produit.ProduitDto;
import com.mfish.erp.produit.ProduitService;
import com.mfish.erp.quartier.QuartierDto;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.CommandeFormulaire;
import com.mfish.erp.web.dto.CommandeVue;
import com.mfish.erp.web.dto.FiltreCommande;
import com.mfish.erp.web.dto.PageVue;
import com.mfish.erp.web.dto.PaiementFormulaire;
import com.mfish.erp.web.dto.StatutFormulaire;
import com.mfish.erp.web.mapper.VueMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Ecran « Commandes » : liste filtrable, creation, fiche, changement de statut, encaissement.
 */
@Controller
@RequestMapping("/commandes")
public class CommandeWebController extends AbstractWebController {

    private final CommandeApplicationService commandeApplicationService;
    private final ClientService clientService;
    private final ProduitService produitService;
    private final QuartierService quartierService;
    private final VueMapper vueMapper;
    private final FlashBuilder flashBuilder;

    public CommandeWebController(final CommandeApplicationService commandeApplicationService,
                                 final ClientService clientService,
                                 final ProduitService produitService,
                                 final QuartierService quartierService,
                                 final VueMapper vueMapper,
                                 final FlashBuilder flashBuilder) {
        this.commandeApplicationService = commandeApplicationService;
        this.clientService = clientService;
        this.produitService = produitService;
        this.quartierService = quartierService;
        this.vueMapper = vueMapper;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String lister(final HttpServletRequest request,
                         @RequestParam(required = false) final String statut,
                         @RequestParam(required = false) final UUID quartierId,
                         @RequestParam(required = false) final LocalDate date,
                         @RequestParam(required = false) final String recherche,
                         @RequestParam(defaultValue = "0") final int page,
                         @RequestParam(defaultValue = "20") final int taille,
                         final Model model) {
        final StatutCommande statutEnum = statut == null || statut.isBlank()
                ? null : StatutCommande.valueOf(statut);
        final FiltreCommande filtre = new FiltreCommande(statut, quartierId, date, recherche, page, taille);
        final Page<CommandeDto> resultat = commandeApplicationService.rechercher(statutEnum, date,
                quartierId, recherche, PageRequest.of(page, taille));
        model.addAttribute("page", pageVue(resultat, filtre));
        model.addAttribute("filtre", filtre);
        model.addAttribute("statuts", StatutCommande.values());
        model.addAttribute("quartiers", quartierService.lister());
        model.addAttribute("pageActive", "commandes");
        return vue(request, "commandes/liste", "commandes/fragments/liste");
    }

    @GetMapping("/nouvelle")
    public String formulaireCreation(final Model model) {
        preparerFormulaire(model);
        return "commandes/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("formulaire") final CommandeFormulaire formulaire,
                        final BindingResult bindingResult,
                        final RedirectAttributes redirectAttributes,
                        final Model model) {
        final List<LigneCommandeRequest> lignes = formulaire.lignes() == null
                ? List.of()
                : formulaire.lignes().stream()
                        .filter(ligne -> ligne.quantite() != null && ligne.quantite() > 0)
                        .map(ligne -> new LigneCommandeRequest(ligne.produitId(), ligne.quantite()))
                        .toList();
        if (bindingResult.hasErrors() || lignes.isEmpty()) {
            if (lignes.isEmpty()) {
                bindingResult.reject("lignes", "validation.auMoinsUneLigne");
            }
            preparerFormulaire(model);
            return "commandes/formulaire";
        }
        final CommandeRequest request = new CommandeRequest(formulaire.clientId(), lignes,
                formulaire.modePaiement(), SourceCommande.MANUEL,
                formulaire.dateLivraisonSouhaitee(), formulaire.notes());
        try {
            final CommandeDto creee = commandeApplicationService.creer(request);
            redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.cree"));
            return "redirect:/commandes/" + creee.id();
        } catch (CommandeInvalideException exception) {
            bindingResult.reject("lignes", exception.getMessage());
            preparerFormulaire(model);
            return "commandes/formulaire";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable final UUID id, final Model model) {
        model.addAttribute("commande", vueMapper.commandeVue(commandeApplicationService.trouver(id),
                clientes(), quartiers(), produits()));
        model.addAttribute("pageActive", "commandes");
        return "commandes/detail";
    }

    @PostMapping("/{id}/statut")
    public String changerStatut(@PathVariable final UUID id,
                                @Valid @ModelAttribute final StatutFormulaire formulaire,
                                final BindingResult bindingResult,
                                final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flash", flashBuilder.erreur("flash.erreur"));
            return "redirect:/commandes/" + id;
        }
        try {
            commandeApplicationService.changerStatut(id, formulaire.statut());
            redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.enregistre"));
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flash",
                    com.mfish.erp.web.dto.FlashMessage.erreur(exception.getMessage()));
        }
        return "redirect:/commandes/" + id;
    }

    @PostMapping("/{id}/paiements")
    public String encaisser(@PathVariable final UUID id,
                            @Valid @ModelAttribute final PaiementFormulaire formulaire,
                            final BindingResult bindingResult,
                            final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("flash", flashBuilder.erreur("flash.erreur"));
            return "redirect:/commandes/" + id;
        }
        try {
            commandeApplicationService.enregistrerPaiement(id, formulaire.montant());
            redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.enregistre"));
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flash",
                    com.mfish.erp.web.dto.FlashMessage.erreur(exception.getMessage()));
        }
        return "redirect:/commandes/" + id;
    }

    @PostMapping("/{id}/annulation")
    public String annuler(@PathVariable final UUID id, final RedirectAttributes redirectAttributes) {
        try {
            commandeApplicationService.annuler(id);
            redirectAttributes.addFlashAttribute("flash", flashBuilder.info("flash.enregistre"));
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("flash",
                    com.mfish.erp.web.dto.FlashMessage.erreur(exception.getMessage()));
        }
        return "redirect:/commandes/" + id;
    }

    private void preparerFormulaire(final Model model) {
        model.addAttribute("clients", clientService.lister());
        model.addAttribute("produits", produitService.lister().stream().filter(ProduitDto::actif).toList());
        if (!model.containsAttribute("formulaire")) {
            model.addAttribute("formulaire", new CommandeFormulaire(null, List.of(), null, null, null));
        }
        model.addAttribute("pageActive", "commandes");
    }

    private PageVue<CommandeVue> pageVue(final Page<CommandeDto> resultat, final FiltreCommande filtre) {
        final Map<UUID, ClientDto> clients = clientes();
        final Map<UUID, QuartierDto> quartiers = quartiers();
        final Map<UUID, ProduitDto> produits = produits();
        final List<CommandeVue> elements = resultat.getContent().stream()
                .map(commande -> vueMapper.commandeVue(commande, clients, quartiers, produits))
                .toList();
        return new PageVue<>(elements, filtre.page(), filtre.taille(), resultat.getTotalElements(),
                resultat.getTotalPages());
    }

    private Map<UUID, ClientDto> clientes() {
        return clientService.lister().stream()
                .collect(Collectors.toMap(ClientDto::id, Function.identity()));
    }

    private Map<UUID, QuartierDto> quartiers() {
        return quartierService.lister().stream()
                .collect(Collectors.toMap(QuartierDto::id, Function.identity()));
    }

    private Map<UUID, ProduitDto> produits() {
        return produitService.lister().stream()
                .collect(Collectors.toMap(ProduitDto::id, Function.identity()));
    }
}


