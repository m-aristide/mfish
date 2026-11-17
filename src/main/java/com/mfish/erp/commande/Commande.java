package com.mfish.erp.commande;

import com.mfish.erp.client.Client;
import com.mfish.erp.commun.AbstractAuditable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Commande passee par un client. Les montants sont des entiers (plus petite unite monetaire).
 * Le champ {@code version} porte le verrou optimiste.
 */
@Entity
@Table(name = "commandes")
public class Commande extends AbstractAuditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCommande statut;

    @Column(name = "date_livraison_souhaitee")
    private LocalDate dateLivraisonSouhaitee;

    @Column(name = "montant_total", nullable = false)
    private long montantTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false)
    private StatutPaiement statutPaiement;

    @Column(name = "montant_paye", nullable = false)
    private long montantPaye;

    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceCommande source;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommande> lignes = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected Commande() {
    }

    public Commande(Client client, StatutCommande statut, ModePaiement modePaiement,
                    StatutPaiement statutPaiement, SourceCommande source) {
        this.client = client;
        this.statut = statut;
        this.modePaiement = modePaiement;
        this.statutPaiement = statutPaiement;
        this.source = source;
    }

    public void ajouterLigne(LigneCommande ligne) {
        ligne.setCommande(this);
        lignes.add(ligne);
    }

    public UUID getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public LocalDate getDateLivraisonSouhaitee() {
        return dateLivraisonSouhaitee;
    }

    public void setDateLivraisonSouhaitee(LocalDate dateLivraisonSouhaitee) {
        this.dateLivraisonSouhaitee = dateLivraisonSouhaitee;
    }

    public long getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(long montantTotal) {
        this.montantTotal = montantTotal;
    }

    public ModePaiement getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(ModePaiement modePaiement) {
        this.modePaiement = modePaiement;
    }

    public StatutPaiement getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(StatutPaiement statutPaiement) {
        this.statutPaiement = statutPaiement;
    }

    public long getMontantPaye() {
        return montantPaye;
    }

    public void setMontantPaye(long montantPaye) {
        this.montantPaye = montantPaye;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public SourceCommande getSource() {
        return source;
    }

    public void setSource(SourceCommande source) {
        this.source = source;
    }

    public List<LigneCommande> getLignes() {
        return lignes;
    }

    public long getVersion() {
        return version;
    }
}
