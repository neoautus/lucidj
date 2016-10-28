package org.lucidj.test;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.lucidj.api.Quark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestActivator implements BundleActivator
{
    private final Logger log = LoggerFactory.getLogger (TestActivator.class);
    private TestServiceTracker tracker;

    public void start (BundleContext context)
            throws Exception
    {
        Quark test_service = new TestService ();

        context.registerService (Quark.class.getName(), test_service, null);

        tracker = new TestServiceTracker (context);
        tracker.open();

        log.info ("Service ComponentFactory started");
    }

    public void stop (BundleContext context)
            throws Exception
    {
        tracker.close();
        tracker = null;
        log.info ("Service ComponentFactory stopped");
    }
}

// EOF
