/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package old.ase.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author Sam Benton
 */
public class PatchAttempt {

    static int count = 0;
    int id;

    Collection<String> ff = new TreeSet();
    Collection<String> fp = new TreeSet();
    Collection<String> pf = new TreeSet();
    Collection<String> pp = new TreeSet();
    Map<String, Collection<Integer>> modifiedMethod = new TreeMap();

    PatchAttempt(Map<String, Collection<Integer>> m) {
        count = count + 1;
        id = count;
        this.modifiedMethod.putAll(m);
    }

    public void save(File dir) throws IOException {
        File f = new File(String.format("%s/tests/%d.tests", dir.getAbsolutePath(), this.id));
        f.getParentFile().mkdirs();
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));

        bw.write(String.format("Patch #%d%n", id));
        bw.write(String.format("ff=%d, fp=%d, pf=%d, pp=%d%n", ff.size(), fp.size(), pf.size(), pp.size()));
        bw.write("------------------\n");

        for (String s : ff) {
            bw.write(s);
            bw.newLine();
        }

        for (String s : fp) {
            bw.write(s);
            bw.newLine();
        }

        for (String s : pf) {
            bw.write(s);
            bw.newLine();
        }

        bw.close();
    }

    public void output() {
        System.out.println(String.format("Patch #%d", id));
        System.out.println(String.format("ff=%d, fp=%d, pf=%d, pp=%d", ff.size(), fp.size(), pf.size(), pp.size()));

        if (fp.size() > 0 && pf.size() == 0) {
            System.out.println("Patch Category = CleanFix");
        } else if (fp.size() > 0 && pf.size() > 0) {
            System.out.println("Patch Category = NoisyFix");
        } else if (fp.size() == 0 && pf.size() == 0) {
            System.out.println("Patch Category = NoneFix");
        } else if (fp.size() == 0 && pf.size() > 0) {
            System.out.println("Patch Category = NegFix");
        }

        for (String k : this.modifiedMethod.keySet()) {
            for (Integer i : this.modifiedMethod.get(k)) {
                System.out.println(String.format("Modified method: %s#%d", k, i));
            }
        }

        System.out.println("--------------");

        for (String s : ff) {
            System.out.println(s);
        }

        for (String s : fp) {
            System.out.println(s);
        }

        for (String s : pf) {
            System.out.println(s);
        }
    }

    public void setFf(Collection<String> ff) {
        this.ff = ff;
    }

    public void setFp(Collection<String> fp) {
        this.fp = fp;
    }

    public void setPf(Collection<String> pf) {
        this.pf = pf;
    }

    public void setPp(Collection<String> pp) {
        this.pp = pp;
    }
}
