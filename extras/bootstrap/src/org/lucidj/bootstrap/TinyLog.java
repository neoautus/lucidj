/*
 * Copyright 2017 NEOautus Ltd. (http://neoautus.com)
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

package org.lucidj.bootstrap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public class TinyLog
{
    private BundleContext context;
    private LogService log_service;

    public TinyLog ()
    {
        context = FrameworkUtil.getBundle (this.getClass ()).getBundleContext ();
    }

    private String conv_str (Object obj)
    {
        if (obj == null)
        {
            return ("null");
        }
        else if (obj instanceof Object[])
        {
            Object[] obj_list = (Object[])obj;
            String result = "";

            for (int i = 0; i < obj_list.length; i++)
            {
                if (!result.isEmpty ())
                {
                    result += ",";
                }
                result += conv_str (obj_list [i]);
            }

            return ("[" + result + "]");
        }

        return (obj.toString ());
    }

    @SuppressWarnings ("unchecked")
    private LogService get_log_service ()
    {
        if (log_service != null)
        {
            try
            {
                // LogService still valid?
                FrameworkUtil.getBundle (log_service.getClass ());
            }
            catch (IllegalStateException oops)
            {
                log_service = null;
            }
        }

        if (log_service == null)
        {
            ServiceReference ref = context.getServiceReference (LogService.class.getName());

            if (ref != null)
            {
                log_service = (LogService) context.getService(ref);
            }
        }

        return (log_service);
    }

    private void write_log (int level, String msg, Object... args)
    {
        int i = 0;

        while (msg.contains ("{}"))
        {
            if (i == args.length)
            {
                break;
            }

            msg = msg.replaceFirst ("\\{\\}", (args [i] == null)? "null": conv_str (args [i]));
            i++;
        }

        Throwable t = null;

        for (i = 0; i < args.length; i++)
        {
            if (args [i] instanceof Throwable)
            {
                // Our beloved ugly stack trace
                t = (Throwable)args [i];
                break;
            }
        }

        if (t == null)
        {
            get_log_service ().log (level, msg);
        }
        else
        {
            get_log_service ().log (level, msg, t);
        }
    }

    public void debug (String msg, Object... args)
    {
        write_log (LogService.LOG_DEBUG, msg, args);
    }

    public void info (String msg, Object... args)
    {
        write_log (LogService.LOG_INFO, msg, args);
    }

    public void warn (String msg, Object... args)
    {
        write_log (LogService.LOG_WARNING, msg, args);
    }

    public void error (String msg, Object... args)
    {
        write_log (LogService.LOG_ERROR, msg, args);
    }
}

// EOF
