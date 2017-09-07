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

package org.lucidj.vaadinui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.UI;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;

public class SmartPush
{
    private final static transient Logger log = LoggerFactory.getLogger (BaseVaadinUI.class);

    private final static int CHECK_INTERVAL = 25;
    private final static int PUSH_INTERVAL = 200;
    private Timer push_timer = new Timer ();
    private UI ui;
    private volatile long prepare_push;

    public SmartPush (UI ui)
    {
        if (!ui.getPushConfiguration().getPushMode().isEnabled())
        {
            log.info ("=============================================");
            log.info ("Push is not ENABLED. Will NOT start SmartPush");
            log.info ("=============================================");
            return;
        }

        if (ui.getPushConfiguration ().getPushMode () != PushMode.MANUAL)
        {
            log.info ("=================================================");
            log.info ("Push mode is not MANUAL. Will NOT start SmartPush");
            log.info ("=================================================");
            return;
        }

        this.ui = ui;

        push_timer = new Timer ();
        push_timer.scheduleAtFixedRate (new TimerTask ()
        {
            @Override
            public void run ()
            {
                timer_poll ();
            }
        }, CHECK_INTERVAL, CHECK_INTERVAL);
    }

    public void stop ()
    {
        if (push_timer != null)
        {
            push_timer.cancel ();
            push_timer = null;
            ui = null;
        }
    }

    // We just check at fixed interval whether there are pending changes to push.
    // When the push interval is meet with pending changes, we lock the session and push.
    private synchronized void timer_poll ()
    {
        if (!ui.isAttached ())
        {
            // We never update a detached UI
            return;
        }

        if (ui.isClosing ())
        {
            // UI.close() issues a final push(), so we can stop right now
            stop ();
            return;
        }

        if (!ui.getConnectorTracker ().hasDirtyConnectors ())
        {
            // Nothing to send, reset and keep waiting
            prepare_push = 0;
        }
        else
        {
            long now = System.currentTimeMillis ();

            // We have data to sync
            if (prepare_push == 0)
            {
                // Start the timer to prepare push
                prepare_push = now;
            }
            else if (now - prepare_push > (PUSH_INTERVAL - CHECK_INTERVAL - 1))
            {
                VaadinSession session = ui.getSession ();
                Lock lock = session.getLockInstance ();

                try
                {
                    lock.lock ();
                    ui.push ();
                }
                finally
                {
                    prepare_push = 0;
                    lock.unlock ();
                }
            }
        }
    }
}

// EOF
