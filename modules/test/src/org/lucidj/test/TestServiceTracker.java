package org.lucidj.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.navigator.View;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class TestServiceTracker extends ServiceTracker
{
    private final Logger log = LoggerFactory.getLogger (TestServiceTracker.class);

    private BundleContext context;

    public TestServiceTracker (BundleContext context)
    {
        super (context, View.class.getName (), null);
        this.context = context;
    }

    public Object addingService (ServiceReference reference)
    {
        View view_service = (View)context.getService (reference);

        log.info ("addingService ({})", view_service);

        return (view_service);
    }

    public void removedService (ServiceReference reference, Object service)
    {
        log.info ("removedService ({})", (View)service);

        super.removedService (reference, service);
    }
}

// EOF
