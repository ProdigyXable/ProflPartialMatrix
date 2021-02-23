/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

import com.mycompany.patchstatistics.tools.Arja;
import com.mycompany.patchstatistics.tools.Astor;
import com.mycompany.patchstatistics.tools.Nopol;
import com.mycompany.patchstatistics.tools.Simfix;
import com.mycompany.patchstatistics.tools.Tbar;
import com.mycompany.patchstatistics.tools.Tool;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 *
 * @author Sam Benton
 */
public class GenerateComparisonStatistics {

    public static void main(String args[]) throws Exception {
        String buggyMethodFile = args[0].trim();
        Collection<String> incorrectMethods = getBuggyMethods(buggyMethodFile);

        String toolDirectory = args[1].trim();

        Tool t;
        String toolFamilyName = args[2].trim().toLowerCase();
        String g = "method";
        
        if(args.length >= 5){
            g = args[4];
        }
        
        if (toolFamilyName.equals("arja")) {
            t = new Arja(toolDirectory, g);
        } else if (toolFamilyName.equals("astor")) {
            t = new Astor(toolDirectory, g);
        } else if (toolFamilyName.equals("nopol")) {
            t = new Nopol(toolDirectory, g);
        } else if (toolFamilyName.equals("simfix")) {
            t = new Simfix(toolDirectory, g);
        } else if (toolFamilyName.equals("tbar")) {
            t = new Tbar(toolDirectory, g);
        } else {
            System.out.println("Failed to detect correct tool name");
            return;
        }

        t.setIncorrectMethod(incorrectMethods);
        t.process(args[3]);
        return;
    }

    public static Collection<String> getBuggyMethods(String path) throws IOException {
        File f = new File(path);
        Collection<String> buffer = Files.readAllLines(f.toPath());
        Collection<String> result = new LinkedList();

        buffer.forEach((s) -> {
            result.add(s.replaceAll(Pattern.quote("^"), ""));
        });

        return result;
    }
}
