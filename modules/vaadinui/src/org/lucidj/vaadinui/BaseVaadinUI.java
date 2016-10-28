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

package org.lucidj.vaadinui;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property;
import com.vaadin.server.ClientConnector;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.Sizeable;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.ConnectorTracker;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.UIDetachedException;
import com.vaadin.ui.VerticalLayout;

import org.lucidj.api.DesktopInterface;
import org.lucidj.shiro.Shiro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.handlers.event.Publishes;
import org.apache.felix.ipojo.handlers.event.publisher.Publisher;

@Theme ("kuori")
@Title ("LucidJ Console")
@Widgetset ("xyz.kuori.CustomWidgetSet")
@StyleSheet ("vaadin://base-ui/styles.css")
@Component (immediate = true)
@Instantiate
@Provides (specifications = UI.class)
@Push (PushMode.MANUAL)
@PreserveOnRefresh
public class BaseVaadinUI extends UI
{
    private final static transient Logger log = LoggerFactory.getLogger (BaseVaadinUI.class);

    private DesktopInterface desktop;
    private ConnectorTracker tracker;

    private VerticalLayout system_toolbar = new VerticalLayout ();
    private HorizontalLayout hSearchArea = new HorizontalLayout ();
    private Layout empty_desktop = new FancyEmptyView ("Empty desktop");
    private int default_sidebar_width_pixels = 250;

    @Requires
    private Shiro shiro;

    @Requires (optional = true, proxy = false, specification = DesktopInterface.class)
    private DesktopInterface base_desktop;

    @Publishes (name = "searchbox", topics = "search", dataKey = "args")
    private Publisher search;

    //=========================================================================================
    // LAYOUTS
    //=========================================================================================

    private MenuBar userSettingsMenu ()
    {
        // User picture + user menu
        final MenuBar settings = new MenuBar();
        //settings.addStyleName("user-menu");

        final MenuBar.MenuItem settingsItem = settings.addItem
        (
            "Willie Coyote",
            new ThemeResource ("../kuori/img/willie-coyote-32.png"),
            null
        );
        settingsItem.addItem("Edit Profile", null, new MenuBar.Command()
        {
            @Override
            public void menuSelected (MenuBar.MenuItem selectedItem)
            {
                log.info ("Edit Profile");
            }
        });
        settingsItem.addItem("Preferences", null, new MenuBar.Command()
        {
            @Override
            public void menuSelected (MenuBar.MenuItem selectedItem)
            {
                log.info ("Preferences");
            }
        });
        settingsItem.addSeparator();
        settingsItem.addItem("Sign Out", FontAwesome.SIGN_OUT, new MenuBar.Command()
        {
            @Override
            public void menuSelected(MenuBar.MenuItem selectedItem)
            {
                VaadinSession.getCurrent().getSession().invalidate();
                Page.getCurrent().reload();
            }
        });

        return (settings);
    }

    private void initSystemToolbar ()
    {
        system_toolbar.setSizeFull();
        system_toolbar.setWidth("100%");

        {
            hSearchArea.setStyleName ("ui-header-area");
            hSearchArea.setWidth (100, Sizeable.Unit.PERCENTAGE);
            hSearchArea.setHeightUndefined ();
            hSearchArea.setDefaultComponentAlignment (Alignment.MIDDLE_LEFT);

            Label title = new Label("&nbsp;", ContentMode.HTML);
            title.addStyleName ("ui-header-logo");
            title.setWidth (default_sidebar_width_pixels, Sizeable.Unit.PIXELS);
            hSearchArea.addComponent (title);

            CssLayout group = new CssLayout();
            group.addStyleName ("v-component-group");
            group.addStyleName ("ui-header-search");
            final ComboBox combo = new ComboBox();
            combo.setInputPrompt("Search or paste URL...");
            //combo.setContainerDataSource(StringGenerator.generateContainer(200, false));
            combo.setNullSelectionAllowed (true);
            combo.setTextInputAllowed (true);
            combo.setNewItemsAllowed (true);
            //combo.select(combo.getItemIds().iterator().next());
            //combo.setItemCaptionPropertyId(StringGenerator.CAPTION_PROPERTY);
            //combo.setItemIconPropertyId(StringGenerator.ICON_PROPERTY);
            combo.setWidth("480px");
            group.addComponent(combo);
            Button search_button = new Button ();
            search_button.setIcon (FontAwesome.SEARCH);
            group.addComponent(search_button);
            hSearchArea.addComponent(group);
            hSearchArea.setExpandRatio (group, 1.0f);

            combo.addValueChangeListener (new Property.ValueChangeListener ()
            {
                @Override
                public void valueChange (Property.ValueChangeEvent valueChangeEvent)
                {
                    String search_args = (String)combo.getValue ();

                    if (search_args != null)
                    {
                        log.info ("SEARCH: {}", search_args);
                        search.sendData (combo.getValue ());
                    }
                }
            });

            MenuBar user = userSettingsMenu ();
            user.addStyleName ("ui-header-user");
            hSearchArea.addComponent (user);
            system_toolbar.addComponent (hSearchArea);
        }

        system_toolbar.addComponent (empty_desktop);
        system_toolbar.setExpandRatio (empty_desktop, 1.0f);
        setContent (system_toolbar);
    }

    //=========================================================================================
    // DESKTOP INSTANCES
    //=========================================================================================

    public DesktopInterface createDesktopInstance ()
    {
        log.info ("base_desktop = " + base_desktop);

        if (base_desktop != null)
        {
            Factory factory = ((Pojo)base_desktop).getComponentInstance ().getFactory ();

            try
            {
                final ComponentInstance new_comp = factory.createComponentInstance (null);

                log.info ("new_comp = " + new_comp.toString ());

                final DesktopInterface new_desktop = (DesktopInterface)((InstanceManager)new_comp).getPojoObject ();

                log.info ("new_desktop = " + new_desktop.toString ());

//                new_desktop.addDetachListener (new ClientConnector.DetachListener ()
//                {
//                    public void detach (ClientConnector.DetachEvent event)
//                    {
//                        log.info ("######### Detached " + new_comp + " ##########");
//                        new_ui.close ();
//                        new_comp.dispose ();
//                    }
//                });

                return (new_desktop);
            }
            catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException e)
            {
                log.info ("createInstance: Exception " + e.toString ());
            }
        }

        return (null);
    }

    //=========================================================================================
    // UI INITIALIZATION
    //=========================================================================================

    private void show_desktop ()
    {
        initSystemToolbar ();

        if ((desktop = createDesktopInstance ()) != null)
        {
            desktop.init (this);
            system_toolbar.replaceComponent (empty_desktop, desktop.getMainLayout ());
        }
    }

    @Override
    protected void init (VaadinRequest vaadinRequest)
    {
        Responsive.makeResponsive (this);
        setLocale (vaadinRequest.getLocale());

        if (vaadinRequest instanceof VaadinServletRequest)
        {
            VaadinServletRequest vsr = (VaadinServletRequest)vaadinRequest;

            // TODO: CAVEATS??
            if ("127.0.0.1".equals (vsr.getLocalAddr ()))
            {
                // Autologin into System when browsing from localhost
                shiro.createSystemSubject ();
            }
        }

        if (!shiro.getSubject ().isAuthenticated ())
        {
            setContent (new Login (shiro, new Login.LoginListener ()
            {
                @Override
                public void loginSuccessful()
                {
                    show_desktop ();
                }
            }));
        }
        else
        {
            show_desktop ();
        }
    }

    //=========================================================================================
    // SMARTPUSH
    //=========================================================================================

    private AtomicInteger calls = new AtomicInteger (0);

    // TODO: REMOVE THIS AFTER SOME TESTING
    @Override
    public void push ()
    {
        int current = calls.incrementAndGet ();
        log.info ("===>>> UI PUSH level = {}", current);
        super.push ();
        log.info ("<<<=== UI PUSH level = {}", current);
        calls.decrementAndGet ();
    }

    class ObservingConnectorTracker extends ConnectorTracker
    {
        private Timer push_timer = new Timer ();
        private transient TimerTask push_task = null;
        private UI ui;

        public ObservingConnectorTracker (UI ui)
        {
            super (ui);
            this.ui = ui;
        }

        // By returning a copy of dirty_list, we avoid getting spurious ConcurrentModificationException
        // on AtmospherePushConnection.java:168. It happens when we are receiving websocket data from
        // navigator at the same time that we are building components, and websocket+push is enabled.
        //
        // java.lang.RuntimeException: Push failed
        //    at com.vaadin.server.communication.AtmospherePushConnection.push(AtmospherePushConnection.java:171)[118:com.vaadin.server:7.6.7]
        //    at com.vaadin.server.communication.PushHandler$2.run(PushHandler.java:150)[118:com.vaadin.server:7.6.7]
        //    at com.vaadin.server.communication.PushHandler.callWithUi(PushHandler.java:243)[118:com.vaadin.server:7.6.7]
        //    at com.vaadin.server.communication.PushHandler.onMessage(PushHandler.java:503)[118:com.vaadin.server:7.6.7]
        //    at com.vaadin.server.communication.PushAtmosphereHandler.onMessage(PushAtmosphereHandler.java:88)[118:com.vaadin.server:7.6.7]
        //    at com.vaadin.server.communication.PushAtmosphereHandler.onRequest(PushAtmosphereHandler.java:78)[118:com.vaadin.server:7.6.7]
        //    at org.atmosphere.cpr.AsynchronousProcessor.action(AsynchronousProcessor.java:199)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.cpr.AsynchronousProcessor.suspended(AsynchronousProcessor.java:107)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.container.Jetty9AsyncSupportWithWebSocket.service(Jetty9AsyncSupportWithWebSocket.java:180)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.cpr.AtmosphereFramework.doCometSupport(AtmosphereFramework.java:2075)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.websocket.DefaultWebSocketProcessor.dispatch(DefaultWebSocketProcessor.java:571)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.websocket.DefaultWebSocketProcessor$3.run(DefaultWebSocketProcessor.java:333)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.util.VoidExecutorService.execute(VoidExecutorService.java:101)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.websocket.DefaultWebSocketProcessor.dispatch(DefaultWebSocketProcessor.java:328)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.websocket.DefaultWebSocketProcessor.invokeWebSocketProtocol(DefaultWebSocketProcessor.java:425)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.atmosphere.container.Jetty9WebSocketHandler.onWebSocketText(Jetty9WebSocketHandler.java:92)[144:com.vaadin.external.atmosphere.runtime:2.2.7.vaadin1]
        //    at org.eclipse.jetty.websocket.common.events.JettyListenerEventDriver.onTextMessage(JettyListenerEventDriver.java:128)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.message.SimpleTextMessage.messageComplete(SimpleTextMessage.java:69)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.events.AbstractEventDriver.appendMessage(AbstractEventDriver.java:65)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.events.JettyListenerEventDriver.onTextFrame(JettyListenerEventDriver.java:122)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.events.AbstractEventDriver.incomingFrame(AbstractEventDriver.java:161)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.WebSocketSession.incomingFrame(WebSocketSession.java:309)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.extensions.ExtensionStack.incomingFrame(ExtensionStack.java:214)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.Parser.notifyFrame(Parser.java:220)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.Parser.parse(Parser.java:258)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.io.AbstractWebSocketConnection.readParse(AbstractWebSocketConnection.java:632)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.websocket.common.io.AbstractWebSocketConnection.onFillable(AbstractWebSocketConnection.java:480)[89:org.eclipse.jetty.websocket.common:9.2.15.v20160210]
        //    at org.eclipse.jetty.io.AbstractConnection$2.run(AbstractConnection.java:544)[73:org.eclipse.jetty.io:9.2.15.v20160210]
        //    at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:635)[84:org.eclipse.jetty.util:9.2.15.v20160210]
        //    at org.eclipse.jetty.util.thread.QueuedThreadPool$3.run(QueuedThreadPool.java:555)[84:org.eclipse.jetty.util:9.2.15.v20160210]
        //    at java.lang.Thread.run(Thread.java:745)[:1.8.0_51]
        // Caused by: java.util.ConcurrentModificationException
        //    at java.util.HashMap$HashIterator.nextNode(HashMap.java:1429)[:1.8.0_51]
        //    at java.util.HashMap$KeyIterator.next(HashMap.java:1453)[:1.8.0_51]
        //    at com.vaadin.server.communication.UidlWriter.write(UidlWriter.java:94)[118:com.vaadin.server:7.6.7]
        //    at com.vaadin.server.communication.AtmospherePushConnection.push(AtmospherePushConnection.java:168)[118:com.vaadin.server:7.6.7]
        //    ... 30 more
        @Override
        public Collection<ClientConnector> getDirtyConnectors()
        {
            return (new HashSet<ClientConnector> (super.getDirtyConnectors ()));
        }

        private synchronized void setup_timer (int delay_ms)
        {
            if (push_task != null)
            {
                log.info ("push_task {} cancel()", push_task);
                push_task.cancel ();
            }

            push_task = new TimerTask ()
            {
                @Override
                public void run ()
                {
                    try
                    {
                        ui.access (new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // Push changes NOW
                                log.info ("*** PUSH CHANGES *** push_task = {}", push_task);

                                synchronized (push_timer)
                                {
                                    if (push_task != null)
                                    {
                                        log.info ("*** VALID PUSH ***");
                                        ui.push ();
                                        push_timer.purge ();
                                        push_task = null;
                                    }
                                    else
                                    {
                                        log.info ("*** NULL PUSH ***");
                                    }
                                }
                            }
                        });
                    }
                    catch (UIDetachedException never_mind)
                    {
                        log.error ("Exception: {}", never_mind);
                    };
                }
            };

            log.info ("push_task {} schedule", push_task);

            push_timer.schedule (push_task, delay_ms);
        }

        @Override
        public void markDirty(ClientConnector connector)
        {
            super.markDirty (connector);

            // The sole purpose of this timer is to make a controlled push scheduling.
            // Usually, with automatic push, ALL changes are directed towards the browser,
            // something that may saturate the comm link if too many changes need to be
            // rendered. With this class AND _manual_ PushMode.MANUAL, the pending changes
            // are synchronized using synchronized batches, minimizing the network load.
            //
            log.debug ("markDirty (connector = {})", connector);
            setup_timer (100);
        }
    }

    @Override
    public ConnectorTracker getConnectorTracker()
    {
        if (tracker == null)
        {
            tracker =  new ObservingConnectorTracker (this);
        }

        return (tracker);
    }

    //=========================================================================================
    // UI EVENTS
    //=========================================================================================

    @Override
    public void attach ()
    {
        log.info ("UI attach: {}", this);

        // Normal detach for everybody
        super.attach ();

        if (desktop != null)
        {
            desktop.detach ();
        }
    }

    @Override
    public void detach ()
    {
        log.info ("UI detach: {}", this);

        if (desktop != null)
        {
            desktop.detach ();
        }

        // Normal detach for everybody
        super.detach();
    }
}

// EOF
