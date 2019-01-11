/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ileInterdite.vues;

import utilitaires.Role;
import ileInterdite.message.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Observable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;
import utilitaires.Action;

/**
 *
 * @author vinetg
 */

 
public class VuePrincipale extends Observable {
    
    private JFrame window;
    
    private JPanel panelCentre =  new JPanel(new BorderLayout());
    
    private ArrayList<JPanel> panelAventuriers;
    private JPanel panelPrincipal = new JPanel(new BorderLayout());
    
    private JButton btnBouger = new JButton("Bouger");
    private JButton btnAssecher=new JButton("Assecher");
    private JButton btnDonner=new JButton("Donner");
    private JButton btnRecuper=new JButton("Recuper");
    private JLabel labelNbPA = new JLabel();
    private JLabel labelNomJoueur = new JLabel("", SwingConstants.CENTER);
   
    
    /**
     * On définit un constructeur de VueAventurier avec une VueGrille v
     * @param v
     */
    public VuePrincipale(VueGrille v, ArrayList<String> nomsAventuriers){
        window = new JFrame();
        window.setSize(1600,800);
        window.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE); 
        window.setTitle("Ile interdite");
        window.add(panelPrincipal);
        
        panelCentre.add(v.getPanelGrille(), BorderLayout.CENTER);
        
        labelNomJoueur.setForeground(Color.WHITE);
        
        JPanel paneGauche=new JPanel(new GridLayout(2,1));
      
        
        JPanel paneDroite=new JPanel(new GridLayout(2,1));
        
        panelPrincipal.add(paneGauche,BorderLayout.WEST);
        panelPrincipal.add(paneDroite,BorderLayout.EAST);
        
       
        
        panelPrincipal.add(panelCentre, BorderLayout.CENTER);
        //=====================================================================
        JPanel panelBoutons = new JPanel(new GridLayout(2,2));
        
        JButton btnAssecher= new JButton("Assecher");
        JButton btnTerminerTour = new JButton("Terminer Tour");
        
        panelBoutons.add(btnBouger);
        panelBoutons.add(btnAssecher);
        panelBoutons.add(btnTerminerTour);
        panelBoutons.add(labelNbPA);
        
        panelCentre.add(panelBoutons, BorderLayout.SOUTH);
                 
        btnBouger.addActionListener((ActionEvent e) -> {
            setChanged();
            notifyObservers(new Message(Action.DEPLACER));
            clearChanged();
        });

        btnAssecher.addActionListener((ActionEvent e) -> {
            setChanged();
            notifyObservers(new Message(Action.ASSECHER));
            clearChanged();
        });

        btnTerminerTour.addActionListener((ActionEvent e) -> {
            setChanged();
            notifyObservers(new Message(Action.TERMINER));
            clearChanged();
        });
        
        
        
        //===================pour chaque aventurier different=================
        
        panelAventuriers=new ArrayList<>();
        
        for(String n: nomsAventuriers){
           VueAventurier va = new VueAventurier(n);
           panelAventuriers.add(va);
           va.setPannelBouttons(panelBoutons);
        }
        
        
         paneGauche.add(panelAventuriers.get(0));
         paneGauche.add(panelAventuriers.get(2));
        
         paneDroite.add(panelAventuriers.get(1));
         paneDroite.add(panelAventuriers.get(3));
        
        
    }
    
    public void actualiserVue(int nbAventurier, String nomJoueur, Role classe, Color couleur, int nombrePA) {
        if (nombrePA == 0) {
            btnBouger.setVisible(false);
        }
        else {
            btnBouger.setVisible(true);
        }
        
        panelPrincipal.setBorder(BorderFactory.createLineBorder(couleur, 2));
        

        panelAventuriers.get(nbAventurier).setBackground(couleur);
        labelNomJoueur.setText(classe + " ( " + nomJoueur + " ) ");
        
        panelCentre.setBorder(new MatteBorder(0, 0, 2, 0, couleur));
        
        labelNbPA.setText("Nombre d'actions restantes : " + nombrePA);
        
        window.setVisible(true);
    }
    
    /**
     * Ferme la fenêtre
     */
    public void close() {
        window.dispose();
    }
}

 



