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

import java.awt.GraphicsEnvironment;
import java.io.File;

public class Main
{
    static String file_separator = System.getProperty ("file.separator");
    static String path_separator = System.getProperty ("path.separator");
    static String exe_suffix = System.getProperty ("os.name").startsWith ("Win")? ".exe": "";
    static String bin_dir = file_separator + "bin" + file_separator;
    static String cache_launcher_dir = file_separator + "cache" + file_separator + "launcher" + file_separator;

    static String rq_home;
    static String jdk_home;

    private static boolean check_path (String path)
    {
        int bin_pos = path.lastIndexOf (bin_dir);

        if (bin_pos != -1)
        {
            // Are we close to home?
            String probable_home = path.substring (0, bin_pos);

            File conf_dir = new File (probable_home + file_separator + "conf");
            File runtime_dir = new File (probable_home + file_separator + "runtime");

            if (conf_dir.exists () && conf_dir.isDirectory () &&
                    runtime_dir.exists () && runtime_dir.isDirectory ())
            {
                // It looks pretty much like home :)
                rq_home = probable_home;
                return (true);
            }
        }

        return (false);
    }

    private static boolean get_system_home ()
    {
        String[] java_class_path = System.getProperty ("java.class.path").split (path_separator);
        String user_dir = System.getProperty ("user.dir") + file_separator;

        for (int i = 0; i < java_class_path.length; i++)
        {
            String classpath_element = java_class_path [i];

            if (!new File (classpath_element).isAbsolute ())
            {
                classpath_element = user_dir + classpath_element;
            }

            if (check_path (classpath_element))
            {
                return (true);
            }
        }

        // We may be using launcher.jar for development stage
        if (check_path (user_dir + "stage" + bin_dir))
        {
            return (true);
        }
        else if (check_path (user_dir.substring (0, user_dir.indexOf (cache_launcher_dir) + 1) + "stage" + bin_dir))
        {
            return (true);
        }

        // Not yet? One last try....
        return (check_path (user_dir));
    }

    private static boolean javac_exists (String possible_jdk_home)
    {
        File javac_file = new File (possible_jdk_home + bin_dir + "javac" + exe_suffix);

        return (javac_file.exists () && !javac_file.isDirectory ());
    }

    private static String find_embedded_jdk ()
    {
        // Not yet. Lets search inside runtime dir
        File[] file_list = new File (rq_home + "/runtime").listFiles();

        for (File file: file_list)
        {
            if (file.isDirectory() && javac_exists (file.getAbsolutePath ()))
            {
                // Embedded jdk_home
                return (file.getAbsolutePath ());
            }
        }

        return (null);
    }

    private static boolean get_jdk_home ()
    {
        // We ignore a possible JAVA_HOME env, since we'll launch Karaf from
        // within this very java process.
        String java_home = System.getProperty ("java.home");

        if (javac_exists (java_home))
        {
            // We found jdk_home
            jdk_home = java_home;
            return (true);
        }
        else // JRE inside JDK?
        {
            int possible_jre_dir_pos = java_home.lastIndexOf (file_separator);

            if (possible_jre_dir_pos != -1)
            {
                java_home = java_home.substring (0, possible_jre_dir_pos);

                if (javac_exists (java_home))
                {
                    // We found jdk_home
                    jdk_home = java_home;
                    return (true);
                }
            }
        }

        // Not yet. Lets search inside runtime dir
        File[] file_list = new File (rq_home + "/runtime").listFiles();

        for (File file: file_list)
        {
            if (file.isDirectory() && javac_exists (file.getAbsolutePath ()))
            {
                // Embedded jdk_home
                jdk_home = file.getAbsolutePath ();
                return (true);
            }
        }

        return (false);
    }

    public static void main (String[] args)
    {
        if (get_system_home ())
        {
            System.out.println ("System Home: '" + rq_home + "'");
        }

        if (get_jdk_home ())
        {
            System.out.println ("JDK Home: '" + jdk_home + "'");
        }

        Launcher.configure (rq_home, jdk_home);

        // TODO: ADD -v --verbose etc
        if (args.length > 0)
        {
            String option = args [0];

            // Shift args
            String[] new_args = new String [args.length - 1];
            System.arraycopy (args, 1, new_args, 0, new_args.length);
            args = new_args;

            switch (option)
            {
                case "start":
                {
                    Launcher.newLauncher ().start (args);
                    break;
                }
                case "stop":
                {
                    Launcher.newLauncher ().stop (args);
                    break;
                }
                case "status":
                {
                    Launcher.newLauncher ().status (args);
                    break;
                }
            }
        }
        else if (GraphicsEnvironment.isHeadless())
        {
            String bundled_jdk;

            // Ok, we got a headless java install. Let's try to remedy this by
            // using our own bundled java, if it's available
            if ((bundled_jdk = find_embedded_jdk ()) != null)
            {
                System.out.println ("Warning: Headless mode detected, trying to use bundled JDK");
                Launcher.configure (rq_home, bundled_jdk);
                Launcher.newLauncher ().launch_gui ();
            }
            else
            {
                // TODO: ALSO WRITE A LOG SOMEWHERE
                System.err.println ("Error: Headless mode detected");
                System.err.println ("Please run Launcher GUI using a compatible JDK");
            }
        }
        else
        {
            // No args, launch the UI
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    new LauncherUI ().setVisible (true);
                }
            });
        }
    }
}

// EOF
