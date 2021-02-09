/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.patchstatistics;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Sam Benton
 */
public class PatchCharacteristic {

    Map<String, Object> characteristics;

    public PatchCharacteristic() {
        this.characteristics = new TreeMap();
    }

    public void defineCharacteristic(String key, Object defaultValue) {
        if (!this.characteristics.containsKey(key)) {
            this.characteristics.put(key, defaultValue);
        }
    }

    public void setCharacteristic(String key, Object value) throws Exception {
        this.keySanityCheck(key);
        this.characteristics.put(key, value);
    }

    public void addElementToCharacteristic(String key, Object element) throws Exception {
        this.keySanityCheck(key);
        Object o = this.characteristics.get(key);
        if (o instanceof Collection) {
            ((List) o).add(element);
        }
    }

    public Object getCharacteristic(String key) {
        return this.characteristics.get(key);
    }

    public Set<String> getKeys() {
        return this.characteristics.keySet();
    }

    public void keySanityCheck(String key) throws Exception {

        return;

//        if (!this.characteristics.containsKey(key)) {
//            throw new KeyException(String.format("Key {%s} missing in characteristics structure", key));
//        }
    }

}
