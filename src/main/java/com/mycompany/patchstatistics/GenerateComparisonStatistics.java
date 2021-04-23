/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

import com.mycompany.patchstatistics.tools.Arja;
import com.mycompany.patchstatistics.tools.Astor;
import com.mycompany.patchstatistics.tools.HistoricalInformation;
import com.mycompany.patchstatistics.tools.Json;
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
        String methodGranularity = "method"; // default granularity

        if (args.length >= 5) {
            methodGranularity = args[4];
        }

        HistoricalInformation history = null;

        if (toolFamilyName.equals("arja")) {
            t = new Arja(toolDirectory, methodGranularity);
        } else if (toolFamilyName.equals("astor")) {
            t = new Astor(toolDirectory, methodGranularity);
        } else if (toolFamilyName.equals("nopol")) {
            t = new Nopol(toolDirectory, methodGranularity);
        } else if (toolFamilyName.equals("simfix")) {
            t = new Simfix(toolDirectory, methodGranularity);
        } else if (toolFamilyName.equals("tbar")) {
            t = new Tbar(toolDirectory, methodGranularity);
        } else if (toolFamilyName.equals("json")) {
            t = new Json(toolDirectory, methodGranularity);
        } else {
            System.out.println("Failed to detect correct tool name");
            return;
        }

        if (args.length >= 6) {
            history = new HistoricalInformation(args[5], t.projectID, methodGranularity, args[3], t.getClass().getSimpleName());
        }

        t.setIncorrectMethod(incorrectMethods);
        t.process(args[3], history);
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
