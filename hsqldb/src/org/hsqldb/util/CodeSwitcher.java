/* Copyrights and Licenses
 *
 * This product includes Hypersonic SQL.
 * Originally developed by Thomas Mueller and the Hypersonic SQL Group. 
 *
 * Copyright (c) 1995-2000 by the Hypersonic SQL Group. All rights reserved. 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 
 *     -  Redistributions of source code must retain the above copyright notice, this list of conditions
 *         and the following disclaimer. 
 *     -  Redistributions in binary form must reproduce the above copyright notice, this list of
 *         conditions and the following disclaimer in the documentation and/or other materials
 *         provided with the distribution. 
 *     -  All advertising materials mentioning features or use of this software must display the
 *        following acknowledgment: "This product includes Hypersonic SQL." 
 *     -  Products derived from this software may not be called "Hypersonic SQL" nor may
 *        "Hypersonic SQL" appear in their names without prior written permission of the
 *         Hypersonic SQL Group. 
 *     -  Redistributions of any form whatsoever must retain the following acknowledgment: "This
 *          product includes Hypersonic SQL." 
 * This software is provided "as is" and any expressed or implied warranties, including, but
 * not limited to, the implied warranties of merchantability and fitness for a particular purpose are
 * disclaimed. In no event shall the Hypersonic SQL Group or its contributors be liable for any
 * direct, indirect, incidental, special, exemplary, or consequential damages (including, but
 * not limited to, procurement of substitute goods or services; loss of use, data, or profits;
 * or business interruption). However caused any on any theory of liability, whether in contract,
 * strict liability, or tort (including negligence or otherwise) arising in any way out of the use of this
 * software, even if advised of the possibility of such damage. 
 * This software consists of voluntary contributions made by many individuals on behalf of the
 * Hypersonic SQL Group.
 *
 *
 * For work added by the HSQL Development Group:
 *
 * Copyright (c) 2001-2002, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer, including earlier
 * license statements (above) and comply with all above license conditions.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, including earlier
 * license statements (above) and comply with all above license conditions.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG, 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.hsqldb.util;

import java.io.*;
import java.util.*;

// fredt@users 20020315 - patch 1.7.0 - minor fixes
// changed line separator to System based value
// moved the Profile class to org.hsqldb.test package

/**
 * changes the source code to support different JDK or profile settings
 *
 * @version 1.7.0
 */
public class CodeSwitcher {

    private static final String ls = System.getProperty("line.separator",
        "\n");
    private Vector           vList;
    private Vector           vSwitchOn;
    private Vector           vSwitchOff;
    private Vector           vSwitches;
    private boolean          bAdd, bRemove;
    private static final int MAX_LINELENGTH = 82;

    /**
     * Method declaration
     *
     *
     * @param a
     */
    public static void main(String a[]) {

        CodeSwitcher s = new CodeSwitcher();

        if (a.length == 0) {
            showUsage();

            return;
        }

        boolean path = false;

        for (int i = 0; i < a.length; i++) {
            String p = a[i];

            if (p.startsWith("+")) {
                if (p.length() == 1) {
                    s.bAdd = true;
                } else {
                    s.vSwitchOn.addElement(p.substring(1));
                }
            } else if (p.startsWith("-")) {
                if (p.length() == 1) {
                    s.bRemove = true;
                } else {
                    s.vSwitchOff.addElement(p.substring(1));
                }
            } else {
                s.addDir(p);

                path = true;
            }
        }

        if (!path) {
            printError("no path specified");
            showUsage();
        }

        s.process();

        if (s.vSwitchOff.size() == 0 && s.vSwitchOn.size() == 0) {
            s.printSwitches();
        }
    }

    /**
     * Method declaration
     *
     */
    static void showUsage() {

        System.out.println(
            "Usage: java CodeSwitcher [paths] [labels] [+][-]");
        System.out.println("If no labels are specified then all used");
        System.out.println("labels in the source code are shown.");
        System.out.println("Use +MODE to switch on the things labeld MODE");
        System.out.println("Use -MODE to switch off the things labeld MODE");
        System.out.println("Path: Any number of path or files may be");
        System.out.println("specified. Use . for the current directory");
        System.out.println("(including sub-directories).");
        System.out.println("Example: java CodeSwitcher +JAVA2 .");
        System.out.println("This example switches on code labeled JAVA2");
        System.out.println("in all *.java files in the current directory");
        System.out.println("and all subdirectories.");
        System.out.println("java CodeSwitcher + .");
        System.out.println("Adds test code to the code.");
        System.out.println("java CodeSwitcher - .");
        System.out.println("Removed test code from the code.");
    }

    /**
     * Constructor declaration
     *
     */
    CodeSwitcher() {

        vList      = new Vector();
        vSwitchOn  = new Vector();
        vSwitchOff = new Vector();
        vSwitches  = new Vector();
    }

    /**
     * Method declaration
     *
     */
    void process() {

        int len = vList.size();

        for (int i = 0; i < len; i++) {
            System.out.print(".");

            String file = (String) vList.elementAt(i);

            if (bAdd || bRemove) {
                int maxlen = testFile(file);

                if (bAdd &&!bRemove) {
                    addTest(file, maxlen);
                } else {
                    removeTest(file);
                }
            } else {
                if (!processFile(file)) {
                    System.out.println("in file " + file + " !");
                }
            }
        }

        System.out.println("");
    }

    /**
     * Method declaration
     *
     */
    void printSwitches() {

        System.out.println("Used labels:");

        for (int i = 0; i < vSwitches.size(); i++) {
            System.out.println((String) (vSwitches.elementAt(i)));
        }
    }

    /**
     * Method declaration
     *
     *
     * @param path
     */
    void addDir(String path) {

        File f = new File(path);

        if (f.isFile() && path.endsWith(".java")) {
            vList.addElement(path);
        } else if (f.isDirectory()) {
            String list[] = f.list();

            for (int i = 0; i < list.length; i++) {
                addDir(path + File.separatorChar + list[i]);
            }
        }
    }

    /**
     * Method declaration
     *
     *
     * @param name
     */
    void removeTest(String name) {

        File f    = new File(name);
        File fnew = new File(name + ".new");

        try {
            LineNumberReader read  = new LineNumberReader(new FileReader(f));
            FileWriter       write = new FileWriter(fnew);

            while (true) {
                String line = read.readLine();

                if (line == null) {
                    break;
                }

                if (line.startsWith("Profile.visit(")) {
                    int s = line.indexOf(';');

                    line = line.substring(s + 1);
                }

                write.write(line + ls);
            }

            read.close();
            write.flush();
            write.close();

            File fbak = new File(name + ".bak");

            fbak.delete();
            f.renameTo(fbak);

            File fcopy = new File(name);

            fnew.renameTo(fcopy);
            fbak.delete();
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    /**
     * Method declaration
     *
     *
     * @param name
     * @param maxline
     */
    void addTest(String name, int maxline) {

        File   f    = new File(name);
        File   fnew = new File(name + ".new");
        String key  = name;

        key = key.replace('\\', '.');

        try {
            LineNumberReader read = new LineNumberReader(new FileReader(f));
            FileWriter       write    = new FileWriter(fnew);
            int              l        = 0;
            boolean          longline = false;

            while (true) {
                String line = read.readLine();

                if (line == null) {
                    break;
                }

                if (line.startsWith(" ")) {
                    int spaces = 0;

                    for (; spaces < line.length(); spaces++) {
                        if (line.charAt(spaces) != ' ') {
                            break;
                        }
                    }

                    if (spaces > 3 && testLine(line) &&!longline) {
                        line = "org.hsqldb.test.Profile.visit(\"" + key
                               + "\"," + l + "," + maxline + ");" + line;

                        l++;
                    } else if (isLongline(line)) {
                        longline = true;
                    } else {
                        longline = false;
                    }
                }

                write.write(line + ls);
            }

            read.close();
            write.flush();
            write.close();

            File fbak = new File(name + ".bak");

            fbak.delete();
            f.renameTo(fbak);

            File fcopy = new File(name);

            fnew.renameTo(fcopy);
            fbak.delete();
        } catch (Exception e) {
            printError(e.getMessage());
        }
    }

    /**
     * Method declaration
     *
     *
     * @param name
     *
     * @return
     */
    int testFile(String name) {

        File f = new File(name);

        try {
            LineNumberReader read = new LineNumberReader(new FileReader(f));
            int              l        = 1,
                             maxline  = 0;
            boolean          longline = false;

            while (true) {
                String line = read.readLine();

                if (line == null) {
                    break;
                }

                if (line.length() > MAX_LINELENGTH
                        &&!line.startsWith("org.hsqldb.test.Profile.")) {
                    System.out.println("long line in " + name + " at line "
                                       + l);
                }

                if (line.startsWith(" ")) {
                    int spaces = 0;

                    for (; spaces < line.length(); spaces++) {
                        if (line.charAt(spaces) != ' ') {
                            break;
                        }
                    }

                    if (spaces > 3 && testLine(line) &&!longline) {
                        maxline++;
                    } else if (isLongline(line)) {
                        longline = true;
                    } else {
                        longline = false;
                    }

                    String s = line.substring(spaces);

                    if (s.startsWith("if(")) {
                        if (!s.endsWith(" {")) {
                            System.out.println("if( without { in " + name
                                               + " at line " + l);
                        }
                    } else if (s.startsWith("} else if(")) {
                        if (!s.endsWith(" {")) {
                            System.out.println("} else if without { in "
                                               + name + " at line " + l);
                        }
                    } else if (s.startsWith("while(")) {
                        if (!s.endsWith(" {")) {
                            System.out.println("while( without { in " + name
                                               + " at line " + l);
                        }
                    } else if (s.startsWith("switch(")) {
                        if (!s.endsWith(" {")) {
                            System.out.println("switch( without { in " + name
                                               + " at line " + l);
                        }
                    } else if (s.startsWith("do ")) {
                        if (!s.endsWith(" {")) {
                            System.out.println("do without { in " + name
                                               + " at line " + l);
                        }
                    }
                }

                l++;
            }

            read.close();

            return maxline;
        } catch (Exception e) {
            printError(e.getMessage());
        }

        return -1;
    }

    /**
     * Method declaration
     *
     *
     * @param line
     *
     * @return
     */
    boolean testLine(String line) {

        if (!line.endsWith(";")) {
            return false;
        }

        if (line.trim().startsWith("super(")) {
            return false;
        }

        return true;
    }

    /**
     * Method declaration
     *
     *
     * @param s
     *
     * @return
     */
    boolean isLongline(String s) {

        char c = s.charAt(s.length() - 1);

        if (",(+-&|".indexOf(c) >= 0) {
            return true;
        }

        return false;
    }

    /**
     * Method declaration
     *
     *
     * @param name
     *
     * @return
     */
    boolean processFile(String name) {

        File    f         = new File(name);
        File    fnew      = new File(name + ".new");
        int     state     = 0;    // 0=normal 1=inside_if 2=inside_else
        boolean switchoff = false;
        boolean working   = false;

        try {
            LineNumberReader read  = new LineNumberReader(new FileReader(f));
            FileWriter       write = new FileWriter(fnew);

            while (true) {
                String line = read.readLine();

                if (line == null) {
                    break;
                }

                if (working) {
                    if (line.equals("/*") || line.equals("*/")) {
                        continue;
                    }
                }

                if (!line.startsWith("//#")) {
                    write.write(line + ls);
                } else {
                    if (line.startsWith("//#ifdef ")) {
                        if (state != 0) {
                            printError(
                                "'#ifdef' not allowed inside '#ifdef'");

                            return false;
                        }

                        write.write(line + ls);

                        state = 1;

                        String s = line.substring(9);

                        if (vSwitchOn.indexOf(s) != -1) {
                            working   = true;
                            switchoff = false;
                        } else if (vSwitchOff.indexOf(s) != -1) {
                            working = true;

                            write.write("/*" + ls);

                            switchoff = true;
                        }

                        if (vSwitches.indexOf(s) == -1) {
                            vSwitches.addElement(s);
                        }
                    } else if (line.startsWith("//#else")) {
                        if (state != 1) {
                            printError("'#else' without '#ifdef'");

                            return false;
                        }

                        state = 2;

                        if (!working) {
                            write.write(line + ls);
                        } else if (switchoff) {
                            write.write("*/" + ls);
                            write.write(line + ls);

                            switchoff = false;
                        } else {
                            write.write(line + ls);
                            write.write("/*" + ls);

                            switchoff = true;
                        }
                    } else if (line.startsWith("//#endif")) {
                        if (state == 0) {
                            printError("'#endif' without '#ifdef'");

                            return false;
                        }

                        state = 0;

                        if (working && switchoff) {
                            write.write("*/" + ls);
                        }

                        write.write(line + ls);

                        working = false;
                    } else {
                        write.write(line + ls);
                    }
                }
            }

            if (state != 0) {
                printError("'#endif' missing");

                return false;
            }

            read.close();
            write.flush();
            write.close();

            File fbak = new File(name + ".bak");

            fbak.delete();
            f.renameTo(fbak);

            File fcopy = new File(name);

            fnew.renameTo(fcopy);
            fbak.delete();

            return true;
        } catch (Exception e) {
            printError(e.getMessage());

            return false;
        }
    }

    /**
     * Method declaration
     *
     *
     * @param error
     */
    static void printError(String error) {
        System.out.println("");
        System.out.println("ERROR: " + error);
    }
}
