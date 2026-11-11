package com.mfish.erp.web.controller;

import com.mfish.erp.client.ClientDto;
import com.mfish.erp.client.ClientService;
import com.mfish.erp.quartier.QuartierDto;
import com.mfish.erp.quartier.QuartierService;
import com.mfish.erp.tournee.ArretTournee;
import com.mfish.erp.tournee.Tournee;
import com.mfish.erp.tournee.TourneeService;
import com.mfish.erp.web.FlashBuilder;
import com.mfish.erp.web.dto.ArretFormulaire;
import com.mfish.erp.web.dto.ArretVue;
import com.mfish.erp.web.dto.TourneeVue;
import com.mfish.erp.web.mapper.VueMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
 * Ecran « Tournee du jour » (prioritaire) : arrets ordonnes, calcul, marquage des arrets.
 */
@Controller
@RequestMapping("/tournee")
public class TourneeWebController extends AbstractWebController {

    private final TourneeService tourneeService;
    private final ClientService clientService;
    private final QuartierService quartierService;
    private final VueMapper vueMapper;
    private final FlashBuilder flashBuilder;

    public TourneeWebController(final TourneeService tourneeService,
                                final ClientService clientService,
                                final QuartierService quartierService,
                                final VueMapper vueMapper,
                                final FlashBuilder flashBuilder) {
        this.tourneeService = tourneeService;
        this.clientService = clientService;
        this.quartierService = quartierService;
        this.vueMapper = vueMapper;
        this.flashBuilder = flashBuilder;
    }

    @GetMapping
    public String tournee(@RequestParam(required = false) final String date, final Model model) {
        preparerTournee(model, dateOuNull(date));
        return "tournee/liste";
    }

    @PostMapping("/calculer")
    public String calculer(@RequestParam(required = false) final String date,
                           final HttpServletRequest request, final RedirectAttributes redirectAttributes,
                           final Model model) {
        final LocalDate cible = dateOuNull(date);
        try {
            if (cible == null) {
                tourneeService.calculer();
            } else {
                tourneeService.calculer(cible);
            }
            redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.tourneeCalculee"));
            if (estHtmx(request)) {
                preparerTournee(model, cible);
                return "tournee/fragments/liste";
            }
            return redirection(cible);
        } catch (RuntimeException exception) {
            if (estHtmx(request)) {
                model.addAttribute("erreurTournee", true);
                preparerTournee(model, cible);
                return "tournee/fragments/liste";
            }
            redirectAttributes.addFlashAttribute("flash", flashBuilder.erreur("tournee.calculErreur"));
            return redirection(cible);
        }
    }

    @PostMapping("/arrets/{id}")
    public String marquerArret(@PathVariable final UUID id,
                               @RequestParam(required = false) final String date,
                               @Valid @ModelAttribute final ArretFormulaire formulaire,
                               final BindingResult bindingResult,
                               final HttpServletRequest request,
                               final RedirectAttributes redirectAttributes,
                               final Model model) {
        final LocalDate cible = dateOuNull(date);
        if (!bindingResult.hasErrors()) {
            tourneeService.marquerArret(id, formulaire.statut(), formulaire.montant());
            redirectAttributes.addFlashAttribute("flash", flashBuilder.succes("flash.enregistre"));
        }
        if (estHtmx(request)) {
            preparerTournee(model, cible);
            return "tournee/fragments/liste";
        }
        return redirection(cible);
    }

    private void preparerTournee(final Model model, final LocalDate date) {
        final LocalDate cible = date != null ? date : tourneeService.tourneeDuJour().getDate();
        final Tournee tournee = tourneeService.tourneeParDate(cible);
        final List<ArretTournee> arrets = tourneeService.arrets(tournee.getId());
        final Map<UUID, ClientDto> clients = clientService.lister().stream()
                .collect(Collectors.toMap(ClientDto::id, Function.identity()));
        final Map<UUID, QuartierDto> quartiers = quartierService.lister().stream()
                .collect(Collectors.toMap(QuartierDto::id, Function.identity()));
        final List<ArretVue> arretsVue = arrets.stream()
                .map(arret -> vueMapper.arretVue(arret, clients, quartiers))
                .toList();
        model.addAttribute("tournee", new TourneeVue(tournee.getId(), tournee.getDate(),
                "statut." + tournee.getStatut().name(), arretsVue));
        model.addAttribute("dateSelectionnee", cible);
        model.addAttribute("pageActive", "tournee");
    }

    private static LocalDate dateOuNull(final String date) {
        return (date == null || date.isBlank()) ? null : LocalDate.parse(date);
    }

    private static String redirection(final LocalDate date) {
        return date == null ? "redirect:/tournee" : "redirect:/tournee?date=" + date;
    }
}
