/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

import java.io.File;
import org.json.simple.JSONObject;

/**
 *
 * @author Sam Benton
 */
public class UnifiedPatchFile implements Comparable<UnifiedPatchFile> {

    final File patch;
    final File test;
    int itemID;
    int sortingID;

    JSONObject jsonString;

    public JSONObject getJSON() {
        return jsonString;
    }

    public File getPatch() {
        return patch;
    }

    public void setSortingID(int sortingID) {
        this.sortingID = sortingID;
    }

    public int getSortingID() {
        return sortingID;
    }

    public File getTest() {
        return test;
    }

    public UnifiedPatchFile(JSONObject jsonString, String name) {
        this.patch = null;
        this.test = null;
        this.jsonString = jsonString;

        itemID = Integer.valueOf(name);
        sortingID = itemID;
    }

    public UnifiedPatchFile(File patch, File test, String name) {
        this.patch = patch;
        this.test = test;

        itemID = Integer.valueOf(name);
        sortingID = itemID;
    }

    public int getItemID() {
        return itemID;
    }

    @Override
    public int compareTo(UnifiedPatchFile upf) {
        return Integer.compare(this.sortingID, upf.sortingID);
    }

}
