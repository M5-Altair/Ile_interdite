/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ileInterdite.model.aventurier;

import utilitaires.Pion;
import ileInterdite.model.Tuile;

/**
 *
 * @author vinetg
 */
public class Messager extends Aventurier {

    /**
     * On définit le constructeur de Messager avec une Tuile tuile et un string nom 
     * @param tuile
     * @param nom
     */
     public Messager(String nom, Tuile tuile){
       super(nom, tuile);   
       setRole(Role.Messager);
       setPion(Pion.GRIS);
    }
     
}
