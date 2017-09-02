/*
 * Copyright 2016 NEOautus Ltd. (http://neoautus.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.lucidj.launcher;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DaemonExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Launcher implements ExecuteResultHandler
{
    static String file_separator = System.getProperty ("file.separator");
    static String path_separator = System.getProperty ("path.separator");
    static String exe_suffix = System.getProperty("os.name").startsWith("Win")? ".exe": "";
    static String bin_dir = file_separator + "bin" + file_separator;

    private String main_class = "org.apache.karaf.main.Main";
    private boolean daemon_mode = true;

    static String system_home;
    static String jdk_home;
    static String java_exe;

    private LauncherWatchdog watchdog;

    private static String find_apache_karaf (String runtime_dir)
    {
        File[] file_array = new File (runtime_dir).listFiles();

        if (file_array == null)
        {
            return (null);
        }

        // Not yet. Lets search inside runtime dir
        List<File> file_list = Arrays.asList (file_array);

        Collections.sort (file_list, Collections.reverseOrder (new AlphanumComparator()));

        for (File file: file_list)
        {
            if (file.isDirectory() && file.getName ().startsWith ("apache-karaf-"))
            {
                // Embedded jdk_home
                return (file.getName ());
            }
        }

        return (null);
    }


    public static void configure (String app_home_path, String jdk_home_path, String user_config)
    {
        system_home = app_home_path;
        jdk_home = jdk_home_path;

        // TODO: CHECK NULL
        String karaf_dirname = find_apache_karaf (system_home + "/runtime");

        // Init Karaf dirs
        String karaf_home = system_home + "/runtime/" + karaf_dirname;
        String karaf_data = system_home + "/cache/" + karaf_dirname;

        System.out.println ("Karaf Home: '" + karaf_home + "'");

        System.setProperty ("system.home", system_home);
        System.setProperty ("system.conf", system_home + "/conf");
        System.setProperty ("user.conf", user_config);
        System.setProperty ("system.bootstrap", system_home + "/runtime/bootstrap");
        System.setProperty ("system.deploy", system_home + "/runtime/application-dev");
        System.setProperty ("java.endorsed.dirs",
            jdk_home + "/jre/lib/endorsed" + path_separator +
            jdk_home + "/lib/endorsed" + path_separator +
            karaf_home + "/lib/endorsed");
        System.setProperty ("java.ext.dirs",
            jdk_home + "/jre/lib/ext" + path_separator +
            jdk_home + "/lib/ext" + path_separator +
            karaf_home + "/lib/ext");
        System.setProperty ("karaf.instances", karaf_home + "/instances");
        System.setProperty ("karaf.home", karaf_home);
        System.setProperty ("karaf.base", karaf_home);
        System.setProperty ("karaf.data", karaf_data);
        System.setProperty ("karaf.etc", system_home + "/conf/" + karaf_dirname);
        System.setProperty ("java.io.tmpdir", karaf_data + "/tmp");
        System.setProperty ("java.util.logging.config.file",
                karaf_home + "/etc/java.util.logging.properties");
        System.setProperty ("karaf.startLocalConsole", "false");
        System.setProperty ("karaf.startRemoteShell", "true");

        // Java executable
        java_exe = jdk_home + bin_dir + "java" + exe_suffix;

        // Launcher JAR
        System.setProperty ("app.launcher.jar",
            Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath ());
    }

    public static Launcher newLauncher ()
    {
        return (new Launcher ());
    }

    //=================================================================================================================
    // PROCESS LAUNCHER
    //=================================================================================================================

    private void addArgument (CommandLine cmdline, String name, String value)
    {
        cmdline.addArgument ("-D" + name + "=" + value, false);
    }

    public static String string_join (String delim, String[] elements)
    {
        StringBuilder sbStr = new StringBuilder();

        for (int i = 0; i < elements.length; i++)
        {
            if (i > 0)
            {
                sbStr.append (delim);
            }
            sbStr.append (elements [i]);
        }
        return sbStr.toString();
    }

    public static String string_join (String delim, List<String> elements)
    {
        String[] elements_array = new String [elements.size ()];
        return (string_join (delim, elements.toArray (elements_array)));
    }

    private boolean launch_cmdline (CommandLine cmdline)
    {
        // TODO: HANDLE CTRL+C SHUTDOWN BUG
        DaemonExecutor executor = new DaemonExecutor ();

        // Do NOT destroy processes on VM exit
        executor.setProcessDestroyer (null);

        // Wait a resonable amount of time for process start
        watchdog = new LauncherWatchdog (15000);
        executor.setWatchdog (watchdog);

        try
        {
            // TODO: DUMP stdout/stderr
            if (daemon_mode)
            {
                // Launch and waits until the process becomes alive
                executor.execute (cmdline, this);
                watchdog.waitForProcessStarted ();
            }
            else
            {
                // Synchronous run
                executor.execute (cmdline);
            }
        }
        catch (Exception e)
        {
            System.out.println ("Exception on exec: " + e.toString ());
        }

        if (watchdog.failureReason () != null)
        {
            System.out.println ("Launcher: Failed: " + watchdog.failureReason ().toString ());
            return (false);
        }

        System.out.println ("Launcher: Successful");
        return (true);
    }

    // TODO: THROW ALL THIS CONFIGS INTO SOME XML/WHATEVER ON CONF DIR
    private void launch (String[] args)
    {
        System.out.println ("Exec: " + java_exe);

        CommandLine cmdline = new CommandLine (java_exe);

        // JVM args
        cmdline.addArgument ("-server");
        cmdline.addArgument ("-Xms128M");
        cmdline.addArgument ("-Xmx1024M");
        cmdline.addArgument ("-XX:+UnlockDiagnosticVMOptions");
        cmdline.addArgument ("-XX:+UnsyncloadClass");
        cmdline.addArgument ("-Djava.awt.headless=true");
        cmdline.addArgument ("-Dcom.sun.management.jmxremote");

        // Get all Karaf boot files
        File[] file_list = new File (System.getProperty ("karaf.home") + "/lib/boot").listFiles ();
        List<String> path_elements = new ArrayList<> ();

        // And add to classpath if found
        if (file_list != null)
        {
            for (int i = 0; i < file_list.length; i++)
            {
                if (file_list [i].isFile () &&
                    file_list [i].getName ().toLowerCase ().endsWith (".jar"))
                {
                    path_elements.add (file_list [i].getAbsolutePath ());
                }
            }

            cmdline.addArgument ("-classpath");
            cmdline.addArgument (string_join (path_separator, path_elements), false);
        }

        // Container args
        addArgument (cmdline, "system.home", System.getProperty ("system.home"));
        addArgument (cmdline, "system.conf", System.getProperty ("system.conf"));
        addArgument (cmdline, "user.conf", System.getProperty ("user.conf"));
        addArgument (cmdline, "system.bootstrap", System.getProperty ("system.bootstrap"));
        addArgument (cmdline, "system.deploy", System.getProperty ("system.deploy"));
        addArgument (cmdline, "java.endorsed.dirs", System.getProperty ("java.endorsed.dirs"));
        addArgument (cmdline, "java.ext.dirs", System.getProperty ("java.ext.dirs"));
        addArgument (cmdline, "karaf.instances", System.getProperty ("karaf.instances"));
        addArgument (cmdline, "karaf.home", System.getProperty ("karaf.home"));
        addArgument (cmdline, "karaf.base", System.getProperty ("karaf.base"));
        addArgument (cmdline, "karaf.data", System.getProperty ("karaf.data"));
        addArgument (cmdline, "karaf.etc", System.getProperty ("karaf.etc"));
        addArgument (cmdline, "java.io.tmpdir", System.getProperty ("java.io.tmpdir"));
        addArgument (cmdline, "java.util.logging.config.file", System.getProperty ("java.util.logging.config.file"));

        // Feature repositories
        String[] features_repositories =
        {
            "mvn:org.apache.shiro/shiro-features/1.2.4/xml/features",
            "mvn:org.lucidj.bootstrap/bootstrap-features/1.0.0/xml/features",
            "mvn:org.apache.felix/org.apache.felix.ipojo.features/1.12.1/xml"
        };

        addArgument (cmdline, "featuresRepositoriesExtra", string_join (",", features_repositories));

        String[] features_boot =
        {
            "bootstrap-core",
            "http",
            "http-whiteboard",
            "ipojo",
            "ipojo-all",
            "ipojo-command",
            "ipojo-webconsole",
            "shiro-core"
        };

        addArgument (cmdline, "featuresBootExtra", string_join (",", features_boot));

        // Class to exec
        cmdline.addArgument (main_class);

        // Add class arguments
        if (args != null)
        {
            for (String arg: args)
            {
                cmdline.addArgument (arg);
            }
        }

        launch_cmdline (cmdline);
    }

    @Override // ExecuteResultHandler
    public void onProcessComplete (int i)
    {
        // Nothing needed
    }

    @Override // ExecuteResultHandler
    public void onProcessFailed (ExecuteException e)
    {
        watchdog.fail (e);
    }

    public void start (String[] args)
    {
        main_class = "org.apache.karaf.main.Main";
        daemon_mode = true;
        launch (args);
    }

    public void stop (String[] args)
    {
        main_class = "org.apache.karaf.main.Stop";
        daemon_mode = false;
        launch (args);
    }

    public void status (String[] args)
    {
        main_class = "org.apache.karaf.main.Status";
        daemon_mode = false;
        launch (args);
    }

    public boolean launch_gui ()
    {
        try
        {
            CommandLine cmdline = new CommandLine (java_exe);
            cmdline.addArgument ("-jar");
            cmdline.addArgument (System.getProperty ("app.launcher.jar"));
            launch_cmdline (cmdline);
            return (true);
        }
        catch (Exception ignore) {};

        return (false);
    }
}

// EOF
