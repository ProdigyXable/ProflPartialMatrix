/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author Sam Benton
 */
public class HistoricalInformation {

    JSONParser parser = new JSONParser();
    public JSONObject jsonData;

    public HistoricalInformation(String arg) {

        try {
            System.out.println(String.format("Loading history from %s", arg));
            
            File data = new File(arg);
            jsonData = (JSONObject) parser.parse(new FileReader(data));
        } catch (IOException ex) {
            System.out.println(String.format("History data from %s does not exist ...", arg));
            System.err.println(ex);
            System.exit(-101);
        } catch (ParseException ex) {
            System.out.println(String.format("Parsing problem with ...", arg));
            System.err.println(ex);
            System.exit(-102);
        }
    }

}
