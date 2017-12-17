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

package org.lucidj.top;

import org.lucidj.api.core.Aggregate;
import org.lucidj.api.core.CodeEngineManager;
import org.lucidj.api.core.DesktopUI;
import org.lucidj.api.core.ServiceContext;
import org.lucidj.api.core.ServiceLocator;
import org.lucidj.api.core.ServiceObject;
import org.lucidj.api.core.ServiceObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.*;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class TopView extends VerticalLayout implements View
{
    private final static Logger log = LoggerFactory.getLogger (TopView.class);

    private IndexedContainer container;
    private Grid grid;
    private Timer update_timer = new Timer ();
    private TimerTask update_task = null;

    @ServiceObject.Context
    private ServiceContext injected_ctx;

    private ServiceContext ctx;

    public TopView (BundleContext bctx, ServiceContext ctx, Map<String, Object> props)
    {
        log.info ("TopView constructor ctx={} bctx={}", ctx, bctx);
        this.ctx = ctx;
    }

    public TopView ()
    {
//        this.ctx = ctx;
    }

    private void updateView ()
    {
//        Set<String> valid_ctxids = new HashSet<> ();
//        TaskContext[] ctx_list = new TaskContext [0];//Kernel.taskManager ().getTaskContexts ();
//
//        for (TaskContext sc: ctx_list)
//        {
//            String ctx_id = sc.toString (); // ...getContextId ();
//            Item item = container.getItem (ctx_id);
//
//            valid_ctxids.add (ctx_id);
//
//            if (item == null)
//            {
//                item = container.addItem (ctx_id);
//            }
//
//            item.getItemProperty ("context_id").setValue (ctx_id);
//            item.getItemProperty ("components").setValue (sc /*.getQuarkContext ()*/.toString ());
//        }
//
//        for (int i = 0; i < container.size (); i++)
//        {
//            String item_id = (String)container.getIdByIndex (i);
//
//            if (!valid_ctxids.contains (item_id))
//            {
//                container.removeItem (item_id);
//            }
//        }
    }

    private void setup_timer (int delay_ms)
    {
        if (update_task != null)
        {
            update_task.cancel ();
        }

        update_task = new TimerTask ()
        {
            @Override
            public void run ()
            {
                updateView ();
            }
        };

        // TODO: MAKE IT REARM
        update_timer.scheduleAtFixedRate (update_task, delay_ms, delay_ms);
        //update_timer.schedule (update_task, delay_ms);

        log.info ("Top started");
    }

    Set<String> testEngines ()
    {
        log.info ("### CONTEXT injected_ctx={} ctx={}", injected_ctx, ctx);
        try (ServiceLocator locator = ctx.newServiceLocator ())
        {
            return (locator.getService (CodeEngineManager.class).getEngines ());
        }
        catch (ServiceObjectException e)
        {
            return (Collections.EMPTY_SET);
        }
    }

    private void buildView()
    {
        setMargin (true);
//        setHeight (100, Unit.PERCENTAGE);
//
//        // Create a container of some type
//        container = new IndexedContainer ();
//
//        // Initialize the container as required by the container type
//        container.addContainerProperty("context_id", String.class, "none");
//        container.addContainerProperty("components", String.class, "none");
//
//        grid = new Grid (container);
//        grid.addStyleName ("top-grid");
//        grid.setWidth (100, Unit.PERCENTAGE);
//        grid.setHeight (100, Unit.PERCENTAGE);
//
//        updateView ();
//
//        //grid.setColumnOrder("name", "born");
//        addComponent(grid);
//
//        setup_timer (3000);

        addComponent (new Label (new Date ().toString ()));

        Object result = testEngines ();

        addComponent (new Label (result == null? "null": result.toString ()));

        DesktopUI dui = Aggregate.adapt (DesktopUI.class, getUI ());

        dui.addListener ("search", new DesktopUI.Listener ()
        {
            @Override
            public void event (String topic, Object eventObject)
            {
                log.info ("DesktopUI.Listener event(): topic={} eventObject={}", topic, eventObject);
                TopView.this.addComponent (new Label ((String)eventObject));
            }
        });
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event)
    {
        // TODO Auto-generated method stub
        if (getComponentCount() == 0)
        {
            buildView();
        }
    }

    @ServiceObject.Validate
    public void validate ()
    {
        // Nothing
        log.info ("validate()");
    }

    @ServiceObject.Invalidate
    public void invalidate ()
    {
        // Nothing
        log.info ("invalidate()");
    }
}

// EOF
