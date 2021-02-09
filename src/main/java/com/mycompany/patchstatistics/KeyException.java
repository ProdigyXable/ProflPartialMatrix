/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

/**
 *
 * @author Sam Benton
 */
public class KeyException extends Exception {

    public KeyException(String string) {
        System.out.println(string);
        System.err.println(string);
    }

}
