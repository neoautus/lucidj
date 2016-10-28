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

package org.lucidj.task;

import org.rationalq.quark.QuarkClassLoader;
import org.rationalq.quark.QuarkSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component (immediate = true)
@Instantiate
@Provides
public class TaskContext
{
    private static final transient Logger log = LoggerFactory.getLogger (TaskContext.class);
    private static InheritableThreadLocal<TaskContext> current_task = new InheritableThreadLocal<> ();

    @Context
    BundleContext ctx;

    private Map<Class, Object> objects = new HashMap<>();

    private ClassLoader cld;
    private QuarkSerializer qsl;
    private Task task;
    private Path source;

    public TaskContext ()
    {
        cld = new TaskClassLoader (ctx);
        qsl = new QuarkSerializer (ctx, cld);
        bindThread ();

        log.info ("TaskContext: {} ctx={} cld={} qsl={}", this, ctx, cld, qsl);
    }

    //================
    // Static methods
    //================

    public static TaskContext currentTaskContext ()
    {
        return (current_task.get ());
    }

    //==============
    // Main methods
    //==============

    public void bindThread ()
    {
        log.info ("Thread {} binded to {}", Thread.currentThread (), this);
        current_task.set (this);
    }

    public BundleContext getBundleContext ()
    {
        return (ctx);
    }

    public <A> A currentTask (Class<A> type)
    {
        if (task != null && task.getClass ().isAssignableFrom (type))
        {
            return ((A)task);
        }

        return (null);
    }

    public Task currentTask ()
    {
        return (currentTask (Task.class));
    }

    public void putObject (Object obj)
    {
        objects.put (obj.getClass (), obj);
    }

    @SuppressWarnings ("unchecked")
    public <T> T getObject (Class<T> obj_class)
    {
        return ((T)objects.get(obj_class));
    }

    //=================
    // Utility methods
    //=================

    public boolean load (Path source_path)
    {
        Charset cs = Charset.forName ("UTF-8");
        Reader reader = null;

        try
        {
            reader = Files.newBufferedReader (source_path, cs);

            log.info ("load (source_path={})", source_path);

            task = (Task)qsl.deserializeObject (reader);
            source = source_path;

            log.info ("load ok task={}", task);
            return (true);
        }
        catch (Exception e)
        {
            log.info ("Error loading task", e);
            return (false);
        }
        finally
        {
            try
            {
                if (reader != null)
                {
                    reader.close();
                }
            }
            catch (Exception ignore) {};
        }
    }

    public boolean save (Path destination_path)
    {
        Charset cs = Charset.forName ("UTF-8");
        Writer writer = null;

        try
        {
            writer = Files.newBufferedWriter (destination_path, cs);

            log.info ("Save: task={} destination_path={}", task, destination_path);

            qsl.serializeObject (writer, task);

            log.info ("Save OK: task={}", task);
            return (true);
        }
        catch (Exception e)
        {
            log.error ("Error saving task", e);
            return (false);
        }
        finally
        {
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
            }
            catch (Exception ignore) {};
        }
    }

    public boolean save ()
    {
        return (save (source));
    }

    private boolean contains_pragma (String value, String pragma)
    {
        if (value != null)
        {
            String pragmas = "," + value.replace (" ", "") + ",";

            return (pragmas.contains ("," + pragma + ","));
        }

        return (false);
    }

    private void start_autoexec ()
    {
/*
        String autoexec_path = SystemConfig.getUserdataDir () + "/" +
                               SystemConfig.getAdminUsername () + "/" +
                               "autoexec.quark";
        Reader reader = null;
        List<Object> object_list = null;
        Map<String, Object> formula_properties = new HashMap<> ();

        boolean load_formula = false;

        File file = new File (autoexec_path);

        try
        {
            reader = new FileReader (file);

            Map<String, Object> formula_metadata = new HashMap<> ();
            QuarkMetadata obj_meta = new QuarkMetadata ();

            // Load metadata first
            if (obj_meta.readObjectMetadata (reader, formula_metadata))
            {
                log.info ("*** Formula metadata: {}", formula_metadata);
                log.info ("Formula serialization_queue: {}", obj_meta.serialization_queue);

                for (Map<String, Object> obj: obj_meta.serialization_queue)
                {
                    if (contains_pragma ((String)obj.get ("Pragmas"), "autoexec"))
                    {
                        load_formula = true;
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.info ("start_autoexec: Exception " + e.toString ());
        }


        if (!load_formula)
        {
            log.info (">>>>>>>>>>>>>>> Skip formula");
            // Skip this formula
            return;
        }

        SystemContext sctx = sapi.getSystemContext (this, "autoexec");
        QuarkSerializer quark = sctx.getQuarkSerializer ();

        try
        {
            reader = new FileReader (file); // reader.reset()...

            // TODO: CREATE INTERFACES
            object_list = (List<Object>)quark.deserializeObject (reader, formula_properties);
        }
        catch (Exception e)
        {
            log.info ("start_autoexec: Exception " + e.toString ());
        }

        sctx.setContextObjects (object_list);

        // Execute all autorun code
        for (Object obj: object_list)
        {
            if (obj instanceof ComponentInterface)
            {
                ComponentInterface ei = (ComponentInterface)obj;

                if (contains_pragma ((String)ei.getProperty ("Pragmas"), "autoexec"))
                {
                    log.info ("*** EditorInterface={} fireEvent", ei);
                    ei.fireEvent (null, "run");
                }
            }
        }

        log.info ("object_list: {}", object_list);
*/
    }

    //======================
    // Internal classloader
    //======================

    public ClassLoader getClassLoader ()
    {
        return (cld);
    }

    public class TaskClassLoader extends QuarkClassLoader
    {
        private TaskClassLoader (BundleContext bundle_context)
        {
            super (bundle_context);
        }

        @Override
        public String toString ()
        {
            return ("TaskClassLoader[" + ctx.getBundle ().getBundleId () + "]");
        }
    }
}

// EOF
