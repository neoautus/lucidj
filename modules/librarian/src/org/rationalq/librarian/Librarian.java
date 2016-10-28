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

package org.rationalq.librarian;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import org.lucidj.api.ComponentInterface;
import org.lucidj.api.Quark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class Librarian implements Quark, ComponentInterface
{
    private final transient Logger log = LoggerFactory.getLogger (Librarian.class);

    private HashMap<String, Object> properties = new HashMap<>();
    private String jar_file_path;

    // https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/src/test/AnalyzerTest.java
    // https://github.com/bndtools/bnd/blob/master/biz.aQute.bndlib.tests/src/test/BuilderTest.java
    // http://bnd.bndtools.org/chapters/600-developer.html
    private void load_jar (String jar_path)
        throws Exception
    {
        String autobundle_dir = "/home/marcond/Lab/rationalq-dev/stage/cache/automatic-bundles/admin/default";

        log.info ("load_jar: " + jar_path);

        Jar output_jar = new Jar (new File (autobundle_dir));  // where our data is

        Builder builder = new Builder ();

        builder.setJar (output_jar);

        // You can provide additional class path entries to allow
        // bnd to pickup export version from the packageinfo file,
        // Version annotation, or their manifests.
        builder.setBase (new File (autobundle_dir));
        //builder.addClasspath (new File (autobundle_dir + "/Bundle-ClassPath/shiro-core-1.2.4.jar"));
        builder.setIncludeResource("Bundle-ClassPath=Bundle-ClassPath");
        builder.setBundleClasspath (".,Bundle-ClassPath/shiro-core-1.2.4.jar");
        builder.addClasspath (new File ("/home/marcond/Lab/rationalq-dev/cache/lib/vaadin/jar/vaadin-server-7.6.3.jar"));
        builder.addClasspath (new File (autobundle_dir));

        builder.setProperty("Bundle-SymbolicName","test-bundle");
        builder.setProperty("Export-Package", "xyz.kuori.shiro, org.apache.shiro");
        builder.setProperty("Bundle-Version","1.0");

        // There are no good defaults so make sure you set the
        // Import-Package
        //builder.setProperty("Import-Package","*");

        Jar build = builder.build ();

        build.write (autobundle_dir + "/built.jar");

        build.getManifest ().write (new FileOutputStream (autobundle_dir + "/MANIFEST.MF"));
    }

    @Override
    public void setProperty (String name, Object value)
    {
        properties.put (name, value);
    }

    @Override
    public Object getProperty (String name)
    {
        return (properties.get (name));
    }

    @Override
    public String getIconTitle ()
    {
        return ("Jar Libraries");
    }

    @Override
    public Object fireEvent (Object source, Object event)
    {
        return (null);
    }

    @Override
    public void setValue (Object value)
    {
        jar_file_path = (String)value;
    }

    @Override
    public Object getValue ()
    {
        return (jar_file_path);
    }

    @Override
    public void deserializeObject(Map<String, Object> properties)
    {
        this.properties.putAll (properties);
        jar_file_path = ((String)properties.get("Jar-File"));
    }

    @Override
    public Map<String, Object> serializeObject ()
    {
        log.info("serializeObject()");
        properties.put ("Jar-File", jar_file_path);
        properties.put ("/", new byte [200]);
        return(properties);
    }
}

// EOF
