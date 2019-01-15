/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ileInterdite.controleur;

import utilitaires.Role;
import ileInterdite.model.aventurier.*;
import utilitaires.*;
import ileInterdite.model.*;
import ileInterdite.message.*;
import ileInterdite.model.cartes.*;
import ileInterdite.vues.*;
import java.util.ArrayList;
import ileInterdite.vues.VuePrincipale;
import java.util.Collections;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author vinetg
 */
public class Controleur implements Observer {

    private VuePrincipale vuePrincipale;
    private VueGrille vueGrille;
    private HashMap<Role, Aventurier> joueurs = new HashMap<>();//à modifier en [4]
    private Grille grille;
    private Aventurier aventurierCourant;
    private ArrayList<CarteInondation> piocheInondation = new ArrayList<>();
    private ArrayList<CarteInondation> defausseInondation = new ArrayList<>();
    private ArrayList<CarteTirage> piocheTirage = new ArrayList<>();
    private ArrayList<CarteTirage> defausseTirage = new ArrayList<>();
    private int niveauEau;
    private ArrayList<Role> listeRoles;

    /**
     * On définit le constructeur du controleur avec une liste d'aventuriers
     * joueurs et une Grille grille
     *
     * @param nomsjoueurs
     * @param roles
     * @param nomTuiles
     */
    public Controleur(ArrayList<String> nomsjoueurs, ArrayList<Role> roles, ArrayList<String> nomTuiles, ArrayList<CarteTirage> pioche, int niveauEau) {
        //Initialisation du niveau d'eau
        this.niveauEau = niveauEau;

        //Initialisation de la Grille
        grille = new Grille(nomTuiles);

        ArrayList<Position> posTuiles = new ArrayList();
        ArrayList<String> nomsTuiles = new ArrayList();
        for (Tuile t : grille.getToutesTuiles()) {
            posTuiles.add(t.getPosition());
            nomsTuiles.add(t.getNom());
        }

        for (String nom : nomsTuiles) {
            piocheInondation.add(new CarteInondation(nom));
        }

        piocheTirage = pioche;

        //initialiser main des joueurs
        initCartes();

        vueGrille = new VueGrille(posTuiles, nomsTuiles);
        vueGrille.addObserver(this);

        //Initialisation des joueurs et du joueur courant
        setRoles(nomsjoueurs, roles);
        aventurierCourant = joueurs.get(listeRoles.get(0));

        for (Role role : joueurs.keySet()) {
            vueGrille.actualiserPositionJoueur(joueurs.get(role).getPosition(), null, joueurs.get(role).getPion());
        }

        // Création des vues
        vuePrincipale = new VuePrincipale(getVueGrille(), listeRoles);
        vuePrincipale.addObserver(this);

        //Ecoute des ICarte
        for (VueAventurier vav : vuePrincipale.getPanelAventuriers().values()) {
            for (ICarte c : vav.getButtonCartes()) {
                c.addObserver(this);
            }
        }

        //Ecoute des IAventurier
        for (VueAventurier av : vuePrincipale.getPanelAventuriers().values()) {
            av.getCarteJoueur().addObserver(this);
        }

        vuePrincipale.actualiserVue(aventurierCourant.getNomJoueur(),
                aventurierCourant.getRole(),
                aventurierCourant.getPion().getCouleur(),
                aventurierCourant.getNbAction()
        );

        //Ecoute des VueDefausse
        for (Role role : joueurs.keySet()) {
            joueurs.get(role).getVueDefausse().addObserver(this);
        }

    }

    public void initCoule() {
        for (int i = 0; i < 6; i++) {
            piocheInondation.get(piocheInondation.size() - 1);
        }
    }

    public void initCartes() {
        for (Role role : joueurs.keySet()) {
            ArrayList<CarteTirage> cartes = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                while (piocheTirage.get(piocheTirage.size() - 1) instanceof CarteMonteeDesEaux) {
                    Collections.shuffle(piocheTirage);
                }
                cartes.add(piocheTirage.get(piocheTirage.size() - 1));
                piocheTirage.remove(piocheTirage.size() - 1);
            }
            try {
                joueurs.get(role).addCartes(cartes);
            } catch (ExceptionAventurier ex) {
            };
            vuePrincipale.getPanelAventuriers().get(role).actualiserVueAventurier(joueurs.get(role).cartesToString());
        }
    }

    /**
     * Fonction globale qui gère le déplacement
     */
    public void gererDeplacement() {
        if (aventurierCourant.getNbAction() > 0) {
            if (aventurierCourant.getRole() == Role.Navigateur) {
                for (Role r : listeRoles) {
                    if (r != Role.Navigateur) {
                        vuePrincipale.getPanelAventuriers().get(r).devenirSuiveur(true);
                    }
                }
            }
            proposerTuiles(aventurierCourant.calculDeplacement(grille), Action.DEPLACER, aventurierCourant.getRole());
        }
    }

    public void appliquerDeplacement(MessagePos messagepos) {
        vueGrille.actualiserPositionJoueur(messagepos.getPos(), aventurierCourant.getPosition(), aventurierCourant.getPion());
        //Si l'aventurier en train de jouer est un pilote
        if (aventurierCourant instanceof Pilote && aventurierCourant.getPouvoir()) {
            Pilote p = (Pilote) aventurierCourant;
            p.setPositionPilote(grille, grille.getTuile(messagepos.getPos()));

        } else {
            aventurierCourant.setTuile(grille.getTuile(messagepos.getPos()));
        }
        aventurierCourant.decremente();
    }

    public void gererNaviguation(Role r) {
        proposerTuiles(joueurs.get(r).calculGuide(grille), Action.SUIVRE, r);
    }

    public void appliquerNavigation(MessagePos messagepos) {
        vueGrille.actualiserPositionJoueur(messagepos.getPos(), joueurs.get(messagepos.getRole()).getPosition(), joueurs.get(messagepos.getRole()).getPion());
        joueurs.get(messagepos.getRole()).setTuile(grille.getTuile(messagepos.getPos()));
        aventurierCourant.decremente();
    }

    /**
     * Fonction globale qui gère l'asséchement
     */
    public void gererAssechement() {
        proposerTuiles(aventurierCourant.calculAssechement(grille), Action.ASSECHER, aventurierCourant.getRole());
    }

    public void appliquerAssechement(MessagePos messagepos) {
        grille.getTuile(messagepos.getPos()).setEtat(EtatTuile.SECHE);
        vueGrille.actualiserEtatTuile(messagepos.getPos(), EtatTuile.SECHE);

        //Si l'aventurier en train de jouer est un ingénieur
        if (aventurierCourant instanceof Ingenieur) {
            //Si le pouvoir de l'ingénieur est utilisable
            if (aventurierCourant.getPouvoir()) {
                aventurierCourant.decremente();
                aventurierCourant.setPouvoir(false);

                gererAssechement();
            } else {
                aventurierCourant.setPouvoir(true);
            }
        } else {
            aventurierCourant.decremente();
        }
    }

    /**
     * Fonction globale qui gère l'inondation de fin de tour
     */
    public void gererInondation() {
        //Pour toutes les cartes à inonder
        for (CarteInondation carteAInonder : tirerCartesInondation()) {
            //Pour toutes les tuiles
            for (Tuile tuile : grille.getToutesTuiles()) {
                //Si La tuile a le même nom que la carte à inonder
                if (tuile.getNom().equals(carteAInonder.getNom())) {

                    try {
                        monteeDesEaux(tuile.getPosition());
                    } catch (ExceptionAventurier ex) {
                        System.out.println("L'aventurier " + ex.getAventurier().getNomJoueur() + " ne peut plus se déplacer !");

                        //à compléter
                    }
                    //Si la tuile est coulée
                    if (tuile.getEtat() != EtatTuile.COULEE) {
                        defausseInondation.add(carteAInonder);
                    }
                }
            }
        }
    }

    /**
     * Augmente de le niveau d'eau d'une tuile, et vérifie qu'aucun aventurier
     * n'est présent sur l'île, et si oui lui propose de se déplacer
     *
     * @param p
     * @throws utilitaires.ExceptionAventurier
     */
    public void monteeDesEaux(Position p) throws ExceptionAventurier {
        //Si le tuile est inondée
        if (getGrille().getTuile(p).getEtat() == EtatTuile.INONDEE) {
            getGrille().getTuile(p).setEtat(EtatTuile.COULEE);
            getVueGrille().actualiserEtatTuile(p, EtatTuile.COULEE);

            for (Role role : joueurs.keySet()) {
                if (joueurs.get(role).getTuile().getEtat() == EtatTuile.COULEE) {
                    if (!joueurs.get(role).calculDeplacement(grille).isEmpty()) {
                        proposerTuiles(joueurs.get(role).calculDeplacement(grille), Action.DEPLACER, role);
                    } else {
                        throw new ExceptionAventurier(joueurs.get(role));
                    }
                }
            }

        } else {
            getGrille().getTuile(p).setEtat(EtatTuile.INONDEE);
            getVueGrille().actualiserEtatTuile(p, EtatTuile.INONDEE);
        }
    }

    //OBLIGERDEPLAEMENT A FAIRE AVEC MESSAGE URGENCE (AZventurier, pos) et TOUS BOUTONS NOIRS SEULEMENT ANNULER
    /**
     * Affiche les cases possibles en les rendant cliquables avec une liste de
     * tuiles et une action
     *
     * @param ct
     * @param act
     */
    public void proposerTuiles(ArrayList<Tuile> ct, Action act, Role role) {
        ArrayList<Position> posTuiles = new ArrayList();

        for (Tuile t : ct) {
            posTuiles.add(t.getPosition());
        }
        getVueGrille().actualiserBoutonsCliquables(posTuiles, act, role);
    }

    /**
     * Passe au prochain joueur
     */
    public void aventurierSuivant() {

        setAventurierCourant(joueurs.get(listeRoles.get((listeRoles.indexOf(aventurierCourant.getRole()) + 1) % joueurs.size())));
    }

    public void actualiserModeleEtVue(Object arg) {

        if (arg instanceof MessagePos) {
            vueGrille.tousBoutonsInertes();
        }

        //Si l'aventurier veux bouger ou suivre le navigateur ou donner une carte, on n'enlève pas la possiblilité de faire déplacer un autre joueur
        if (!(((Message) arg).getAction() == Action.DEPLACER && !(arg instanceof MessagePos)) && !(((Message) arg).getAction() == Action.SUIVRE && !(arg instanceof MessagePos))) {
            for (Role r : listeRoles) {
                vuePrincipale.getPanelAventuriers().get(r).devenirSuiveur(false);
            }
        }

        vuePrincipale.actualiserVue(getAventurierCourant().getNomJoueur(),
                getAventurierCourant().getRole(),
                getAventurierCourant().getPion().getCouleur(),
                getAventurierCourant().getNbAction()
        );

        ArrayList<Tuile> casesAssechables = getAventurierCourant().calculAssechement(getGrille());
        // Si l'aventurier a moins de 1 action ou qu'il n'est pas un ingénieur qui utilise son pouvoir et qui a encore des cases possibles a assécher
        if (getAventurierCourant().getNbAction() < 1
                && !(aventurierCourant instanceof Ingenieur
                && !(aventurierCourant.getPouvoir())
                && !casesAssechables.isEmpty())) {
            nextTurn();
        }
    }

    /**
     * Change de tour : remet les points d'action a 3, remet le pouvoir en
     * utilisable et actualise la vuePrincipale avec les paramètres du nouvel
     * aventurier
     */
    public void nextTurn() {
        aventurierCourant.reset();
        tirerCartes();
        gererInondation();
        aventurierSuivant();
        vuePrincipale.actualiserVue(getAventurierCourant().getNomJoueur(),
                getAventurierCourant().getRole(),
                getAventurierCourant().getPion().getCouleur(),
                getAventurierCourant().getNbAction()
        );
    }

    public void appliquerDeffausse(MessageCarte messageCarte) {
        CarteTirage carteSelection = null;
        for (CarteTirage carte : joueurs.get(messageCarte.getRole()).getCartes()) {
            if (carte.getNom().equals(messageCarte.getNomCarte())) {
                carteSelection = carte;
            }

        }
        if (carteSelection.getUtilisable()) {
            if (carteSelection instanceof CarteHelicoptere) {
                //à compléter
            } else {
                //à compléter
            }
        }

        defausseTirage.add(carteSelection);
        joueurs.get(messageCarte.getRole()).removeCarte(carteSelection);
        vuePrincipale.getPanelAventuriers().get(messageCarte.getRole()).actualiserVueAventurier(joueurs.get(messageCarte.getRole()).cartesToString());
        joueurs.get(messageCarte.getRole()).getVueDefausse().close();
    }

    public void appliquerDon(MessageCarte messageCarte) {
        for (Role aventurier : joueurs.keySet()) {
            if (aventurier != aventurierCourant.getRole() && (aventurierCourant.getTuile() == joueurs.get(aventurier).getTuile() || aventurierCourant.getRole() == Role.Messager)) {
                vuePrincipale.getPanelAventuriers().get(aventurier).devenirReceveur(messageCarte.getNomCarte(), true);
            }
        }
    }

    public void appliquerRecevoir(MessageCarte messageCarte) {
        CarteTirage carte = stringToCarte(messageCarte.getNomCarte());
        ArrayList<CarteTirage> cartes = new ArrayList<>();
        cartes.add(carte);
        aventurierCourant.getCartes().remove(carte);
        try {
            joueurs.get(messageCarte.getRole()).addCartes(cartes);
        } catch (ExceptionAventurier ex) {

            joueurs.get(messageCarte.getRole()).defausseCartes();
        }
        vuePrincipale.getPanelAventuriers().get(messageCarte.getRole()).actualiserVueAventurier(joueurs.get(messageCarte.getRole()).cartesToString());
        vuePrincipale.getPanelAventuriers().get(aventurierCourant.getRole()).actualiserVueAventurier(aventurierCourant.cartesToString());

        aventurierCourant.decremente();
    }

    /**
     * S'occupe de toute les opérations(logique applicative)
     *
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {

        if (arg instanceof Message) {
            Message message = (Message) arg;

            //Si le message contient une Action
            if (null != message.getAction()) {
                vueGrille.tousBoutonsInertes();
                switch (message.getAction()) {
                    //Si le message possède l'action ASSECHER
                    case ASSECHER:
                        gererAssechement();
                        break;
                    //Si le message possède l'action DEPLACER
                    case DEPLACER:
                        gererDeplacement();
                        break;
                    //Si le message possède l'action TERMINER
                    case TERMINER:
                        nextTurn();
                        break;
                    case DONNER:
                        gererDon();
                        break;
                    case SUIVRE:
                        gererNaviguation(message.getRole());
                        break;
                    case RECUPERER_TRESOR:
                        gererRecupTresor();

                }
            }
        }
        //Si arg est de type MessagePos
        if (arg instanceof MessagePos) {
            MessagePos messagepos = (MessagePos) arg;
            switch (messagepos.getAction()) {
                case DEPLACER:
                    appliquerDeplacement(messagepos);
                    break;
                case SUIVRE:
                    appliquerNavigation(messagepos);
                    break;
                case ASSECHER:
                    appliquerAssechement(messagepos);
                    break;
            }
        }
        if (arg instanceof MessageCarte) {
            MessageCarte messageCarte = (MessageCarte) arg;
            switch (messageCarte.getAction()) {
                case DEFAUSSER:
                    appliquerDeffausse(messageCarte);
                    break;
                case DONNER:
                    appliquerDon(messageCarte);
                    break;
                case RECEVOIR:
                    appliquerRecevoir(messageCarte);
                    break;
            }
        }
        actualiserModeleEtVue(arg);
    }

    public CarteTirage stringToCarte(String nomCarte) {
        CarteTirage carteSelection = null;
        for (CarteTirage carte : aventurierCourant.getCartes()) {
            if (carte.getNom() == nomCarte) {
                carteSelection = carte;
            }
        }
        return carteSelection;
    }

    /**
     *
     * @param nomsJoueurs
     * @param Rôles
     */
    public void setRoles(ArrayList<String> nomsJoueurs, ArrayList<Role> Rôles) {
        for (Tuile t : getGrille().getTuiles().values()) {
            for (int i = 0; i < nomsJoueurs.size(); i++) {
                if (t.getNom().equals(Rôles.get(i).getCaseDepart())) {
                    getJoueurs().put(Rôles.get(i), créerAventurier(t, nomsJoueurs.get(i), Rôles.get(i)));
                }
            }
        }
        listeRoles = new ArrayList<>(joueurs.keySet());

    }

    public Aventurier créerAventurier(Tuile t, String n, Role r) {
        Aventurier a = null;
        switch (r) {
            case Explorateur:
                a = new Explorateur(n, t);
                break;
            case Messager:
                a = new Messager(n, t);
                break;
            case Plongeur:
                a = new Plongeur(n, t);
                break;
            case Navigateur:
                a = new Navigateur(n, t);
                break;
            case Pilote:
                a = new Pilote(n, t);
                break;
            case Ingénieur:
                a = new Ingenieur(n, t);
                break;
        }
        return a;
    }

    /**
     * On remet les cartes de la défausse sur le haut de la pioche des cartes
     * d'inondation puis on pioches dans cette pioche un nombre de cartes
     * dépendant du niveau d'eau
     *
     * @return une liste de CarteInondation
     */
    public ArrayList<CarteInondation> tirerCartesInondation() {
        ArrayList<CarteInondation> cartesTirees = new ArrayList<>();
        //1,2 -> 2 cartes
        //3,4,5 -> 3 cartes
        //6,7 -> 4 cartes
        //8,9 -> 5 cartes
        int j;
        if (niveauEau < 8) {
            j = 2 + niveauEau / 3;
        } else {
            j = 5;
        }

        //Pour chauqe niveau d'eau
        for (int i = 0; i < j; i++) {
            //Si la pioche inondation n'est pas vide
            if (!piocheInondation.isEmpty()) {
                cartesTirees.add(piocheInondation.get(piocheInondation.size() - 1));
                piocheInondation.remove(piocheInondation.size() - 1);
                //Si la pioche inondation est vide
            } else {
                Collections.shuffle(defausseInondation);
                piocheInondation.addAll(defausseInondation);
                defausseInondation.clear();
            }
        }
        return cartesTirees;
    }

    /**
     * On tire 2 cartes qu'on donne a l'aventurier courant
     */
    public void tirerCartes() {
        ArrayList<CarteTirage> cartes = new ArrayList<>();
        Boolean trigger = false;
        //Pour le nombre de cartes qu'on veut donner
        for (int i = 0; i < 2; i++) {
            //Si la pioche n'est pas vide
            if (!piocheTirage.isEmpty()) {
                //Si la prochaine carte est une carte montée des eaux
                if (piocheTirage.get(piocheTirage.size() - 1) instanceof CarteMonteeDesEaux) {
                    trigger = true;
                    niveauEau += 1;
                    defausseTirage.add(piocheTirage.get(piocheTirage.size() - 1));
                } //Si la prochaine carte n'est pas une carte montée des eaux
                else {
                    cartes.add(piocheTirage.get(piocheTirage.size() - 1));
                }
                piocheTirage.remove(piocheTirage.get(piocheTirage.size() - 1));
                // Si la pioche est vide    
            } else {
                Collections.shuffle(defausseTirage);
                piocheTirage.addAll(defausseTirage);
                defausseTirage.clear();
            }

        }
        if (trigger) {
            Collections.shuffle(defausseInondation);
            piocheInondation.addAll(defausseInondation);
            defausseInondation.clear();
        }
        try {
            aventurierCourant.addCartes(cartes);
        } catch (ExceptionAventurier e) {
            aventurierCourant.defausseCartes();

        }

        vuePrincipale.getPanelAventuriers().get(aventurierCourant.getRole()).actualiserVueAventurier(joueurs.get(aventurierCourant.getRole()).cartesToString());
    }

    public void gererDon() {
        Boolean yes = false;
        if (aventurierCourant.getRole() == Role.Messager) {
            yes = true;
        }
        for (Role role : joueurs.keySet()) {
            if (joueurs.get(role).getTuile().equals(aventurierCourant.getTuile()) && !joueurs.get(role).equals(aventurierCourant)) {
                yes = true;
            }
        }
        if (yes) {
            vuePrincipale.getPanelAventuriers().get(aventurierCourant.getRole()).rendreCartesCliquables(aventurierCourant.cartesTresor());
        }
    }

    private void gererRecupTresor() {
        if (aventurierCourant.peutRecupererTresor()){
            System.out.println("on passe a recup");
            recupererTresor();
        }
            
    }
    

    public void recupererTresor(){
        System.out.println("debut recup");
        ArrayList<CarteTirage>  listeCartesTresor=new ArrayList<>();
        if (aventurierCourant.getTuile().getNom().equals("Le Temple de La Lune") || aventurierCourant.getTuile().getNom().equals("Le Temple du Soleil")){
            System.out.println("1p");
            Tresor.PIERRE.setRecuperé(true);
            checkImage(Tresor.PIERRE);
            for (CarteTirage carte :aventurierCourant.getCartes()){
                System.out.println("2p");
                    if (carte.getNom().equals(Tresor.PIERRE.toString()) && listeCartesTresor.size()<=4){
                        listeCartesTresor.add(carte);
                    }
                }
            System.out.println("Pierre Récupérée");
        } else if (aventurierCourant.getTuile().getNom().equals("Le Palais des Marees") ||aventurierCourant.getTuile().getNom().equals("Le Palais de Corail")){
            System.out.println("1ca");
            Tresor.CALICE.setRecuperé(true);
            checkImage(Tresor.CALICE);
            for (CarteTirage carte :aventurierCourant.getCartes()){
                System.out.println("2ca");
                    if (carte.getNom().equals(Tresor.CALICE.toString()) && listeCartesTresor.size()<=4){
                        listeCartesTresor.add(carte);
                    }
                }
            System.out.println("Calice Récupéré");
        }else if (aventurierCourant.getTuile().getNom().equals("La Caverne des Ombres") || aventurierCourant.getTuile().getNom().equals("La Caverne du Brasier")){
            System.out.println("1cr");
            Tresor.CRISTAL.setRecuperé(true);
            checkImage(Tresor.CRISTAL); 
            for (CarteTirage carte :aventurierCourant.getCartes()){
                System.out.println("2cr");
                    if (carte.getNom().equals(Tresor.CRISTAL.toString()) && listeCartesTresor.size()<=4){
                        listeCartesTresor.add(carte);
                    }
                }
            System.out.println("Cristal Récupéré");
        } else if (aventurierCourant.getTuile().getNom().equals("Le Jardin des Hurlements") || aventurierCourant.getTuile().getNom().equals("Le Jardin des Murmures")){
            System.out.println("1z");
            Tresor.ZEPHYR.setRecuperé(true);
            checkImage(Tresor.ZEPHYR); 
            
                for (CarteTirage carte :aventurierCourant.getCartes()){
                System.out.println("2z");
                    if (carte.getNom().equals(Tresor.ZEPHYR.toString()) && listeCartesTresor.size()<=4){
                        listeCartesTresor.add(carte);
                    }
                }
            System.out.println("Zephyr Récupéré");
        }
            System.out.println("fin de recuperer");
            
        for (CarteTirage carte : listeCartesTresor){    
            aventurierCourant.removeCarte(carte);
        }
        vuePrincipale.getPanelAventuriers().get(aventurierCourant.getRole()).actualiserVueAventurier(aventurierCourant.cartesToString());
        aventurierCourant.decremente();
    }
    
        
    private void checkImage(Tresor tresor) {
        //à compléter
    }

    //Getters et Setters :
    /**
     * @return the joueurs
     */
    public HashMap<Role, Aventurier> getJoueurs() {
        return joueurs;
    }

    /**
     * @param joueurs the joueurs to set
     */
    public void setJoueurs(HashMap<Role, Aventurier> joueurs) {
        this.joueurs = joueurs;
    }

    /**
     * @return the vuePrincipale
     */
    public VuePrincipale getvuePrincipale() {
        return vuePrincipale;
    }

    /**
     * @param vuePrincipale the vuePrincipale to set
     */
    public void setvuePrincipale(VuePrincipale vuePrincipale) {
        this.vuePrincipale = vuePrincipale;
    }

    /**
     * @return the vueGrille
     */
    public VueGrille getVueGrille() {
        return vueGrille;
    }

    /**
     * @param vueGrille the vueGrille to set
     */
    public void setVueGrille(VueGrille vueGrille) {
        this.vueGrille = vueGrille;
    }

    /**
     * @return the grille
     */
    public Grille getGrille() {
        return grille;
    }

    /**
     * @param grille the grille to set
     */
    public void setGrille(Grille grille) {
        this.grille = grille;
    }

    /**
     * @return the aventurierCourant
     */
    public Aventurier getAventurierCourant() {
        return aventurierCourant;
    }

    /**
     * @param aventurierCourant the aventurierCourant to set
     */
    public void setAventurierCourant(Aventurier aventurierCourant) {
        this.aventurierCourant = aventurierCourant;
    }

    /**
     * @return the niveauEau
     */
    public int getNiveauEau() {
        return niveauEau;
    }

    /**
     * @param niveauEau the niveauEau to set
     */
    public void setNiveauEau(int niveauEau) {
        this.niveauEau = niveauEau;
    }

    /**
     * @param piocheInondation the piocheInondation to set
     */
    public void setPiocheInondation(ArrayList<CarteInondation> piocheInondation) {
        this.piocheInondation = piocheInondation;
    }

    /**
     * @param defausseInondation the defausseInondation to set
     */
    public void setDefausseInondation(ArrayList<CarteInondation> defausseInondation) {
        this.defausseInondation = defausseInondation;
    }

    /**
     * @param piocheTirage the piocheTirage to set
     */
    public void setPiocheTirage(ArrayList<CarteTirage> piocheTirage) {
        this.piocheTirage = piocheTirage;
    }

    /**
     * @param defausseTirage the defausseTirage to set
     */
    public void setDefausseTirage(ArrayList<CarteTirage> defausseTirage) {
        this.defausseTirage = defausseTirage;
    }

    /**
     * @return the piocheInondation
     */
    public ArrayList<CarteInondation> getPiocheInondation() {
        return piocheInondation;
    }

    /**
     * @return the defausseInondation
     */
    public ArrayList<CarteInondation> getDefausseInondation() {
        return defausseInondation;
    }

    /**
     * @return the piocheTirage
     */
    public ArrayList<CarteTirage> getPiocheTirage() {
        return piocheTirage;
    }

    /**
     * @return the defausseTirage
     */
    public ArrayList<CarteTirage> getDefausseTirage() {
        return defausseTirage;
    }

}
