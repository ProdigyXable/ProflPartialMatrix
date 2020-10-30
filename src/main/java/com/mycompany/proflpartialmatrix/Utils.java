package com.mycompany.proflpartialmatrix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Sam Benton
 */
public class Utils {

    public static boolean writeToFile(File f, Collection<String> messages) {
        f.getParentFile().mkdirs();
        System.out.println("Creating file: " + f);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            for (String m : messages) {
                bw.write(m);
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println("writeToFile exception: ");
            System.out.println(e.getMessage());
        }

        return true;
    }

}
