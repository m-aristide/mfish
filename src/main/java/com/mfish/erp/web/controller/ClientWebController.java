package com.mfish.erp.web.controller;

import com.mfish.erp.client.ClientRequest;
import com.mfish.erp.client.ClientService;
import com.mfish.erp.commande.CommandeApplicationService;
import com.mfish.erp.quartier.QuartierDto;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.ClientFormulaire;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
 * Ecran « Clients » : liste avec recherche, creation, fiche, modification, archivage.
 */
@Controller
@RequestMapping("/clients")
public class ClientWebController extends AbstractWebController {

    private final ClientService clientService;
    private final QuartierService quartierService;
    private final CommandeApplicationService commandeApplicationService;
    private final FlashBuilder flashBuilder;

    public ClientWebController(final ClientService clientService,
                               final QuartierService quartierService,
                               final CommandeApplicationService commandeApplicationService,
                               final FlashBuilder flashBuilder) {
        this.clientService = clientService;
        this.quartierService = quartierService;
        this.commandeApplicationService = commandeApplicationService;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String lister(final HttpServletRequest request,
                         @RequestParam(required = false) final String recherche,
                         @RequestParam(defaultValue = "0") final int page,
                         @RequestParam(defaultValue = "20") final int taille,
                         final Model model) {
        model.addAttribute("page", clientService.rechercher(recherche, PageRequest.of(page, taille)));
        model.addAttribute("quartierNoms", quartierNoms());
        model.addAttribute("recherche", recherche);
        model.addAttribute("pageActive", "clients");
        return vue(request, "clients/liste", "clients/fragments/liste");
    }

    @GetMapping("/nouveau")
    public String formulaireCreation(final Model model) {
        preparerFormulaire(model, new ClientFormulaire(null, null, null, null, null, null, null, null));
        return "clients/formulaire";
    }

    @PostMapping
    public String creer(@Valid @ModelAttribute("formulaire") final ClientFormulaire formulaire,
                        final BindingResult bindingResult, final Model model,
                        final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, formulaire);
            return "clients/formulaire";
        }
        final var cree = clientService.creer(requete(formulaire));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.cree"));
        return "redirect:/clients/" + cree.id();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable final UUID id, final Model model) {
        final var client = clientService.trouver(id);
        final String quartierNom = client.quartierId() == null
                ? null : quartierService.trouver(client.quartierId()).nom();
        model.addAttribute("client", client);
        model.addAttribute("quartierNom", quartierNom);
        model.addAttribute("commandes", commandeApplicationService.listerParClient(id));
        model.addAttribute("pageActive", "clients");
        return "clients/detail";
    }

    @GetMapping("/{id}/modifier")
    public String formulaireModification(@PathVariable final UUID id, final Model model) {
        final var client = clientService.trouver(id);
        preparerFormulaire(model, new ClientFormulaire(client.nom(), client.telephone(),
                client.quartierId(), client.adresse(), client.repere(), client.latitude(),
                client.longitude(), client.notes()));
        model.addAttribute("clientId", id);
        return "clients/formulaire";
    }

    @PostMapping("/{id}")
    public String modifier(@PathVariable final UUID id,
                           @Valid @ModelAttribute("formulaire") final ClientFormulaire formulaire,
                           final BindingResult bindingResult, final Model model,
                           final RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            preparerFormulaire(model, formulaire);
            model.addAttribute("clientId", id);
            return "clients/formulaire";
        }
        clientService.modifier(id, requete(formulaire));
        redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.modifie"));
        return "redirect:/clients/" + id;
    }

    @PostMapping("/{id}/archiver")
    public String archiver(@PathVariable final UUID id, final RedirectAttributes redirectAttributes) {
        clientService.archiver(id);
        redirectAttributes.addFlashAttribute("flash", flashBuilder.info("flash.enregistre"));
        return "redirect:/clients";
    }

    @PostMapping("/{id}/desarchiver")
    public String desarchiver(@PathVariable final UUID id, final RedirectAttributes redirectAttributes) {
        clientService.desarchiver(id);
        redirectAttributes.addFlashAttribute("flash", flashBuilder.info("flash.enregistre"));
        return "redirect:/clients";
    }

    private ClientRequest requete(final ClientFormulaire formulaire) {
        return new ClientRequest(formulaire.nom(), formulaire.telephone(), formulaire.quartierId(),
                formulaire.adresse(), formulaire.repere(), formulaire.latitude(),
                formulaire.longitude(), formulaire.notes());
    }

    private void preparerFormulaire(final Model model, final ClientFormulaire formulaire) {
        model.addAttribute("formulaire", formulaire);
        model.addAttribute("quartiers", quartierService.lister());
        model.addAttribute("pageActive", "clients");
    }

    private Map<UUID, String> quartierNoms() {
        return quartierService.lister().stream()
                .collect(Collectors.toMap(QuartierDto::id, QuartierDto::nom));
    }
}

