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

package org.lucidj.api;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import java.util.Map;

public interface NavigatorManager
{
    String ATTR_VIEW_PROVIDER = NavigatorManager.class.getName () + ".view-provider";

    boolean configureNavigator (Navigator navigator, Map<String, Object> properties);
    ViewProvider findViewProvider   (String navigationState);

    static ViewProvider getViewProvider ()
    {
        UI current_ui = UI.getCurrent ();

        if (current_ui == null)
        {
            return (null);
        }

        VaadinSession current_session = current_ui.getSession ();

        if (current_session == null)
        {
            return (null);
        }

        Object view_provider_obj = current_session.getAttribute (ATTR_VIEW_PROVIDER);

        if (view_provider_obj instanceof ViewProvider)
        {
            return ((ViewProvider)view_provider_obj);
        }
        return (null);
    }

    static View getOrCreateView (String navigationState)
    {
        ViewProvider view_provider = getViewProvider ();

        if (view_provider == null)
        {
            return (null);
        }

        String view_name = view_provider.getViewName (navigationState);

        if (view_name == null)
        {
            return (null);
        }
        return (view_provider.getView (view_name));
    }

    static <T> T getOrCreateView (String navigationState, Class<T> type)
    {
        Object view = getOrCreateView (navigationState);

        if (view == null || type == null)
        {
            return (null);
        }
        else if (type.isAssignableFrom (view.getClass ()))
        {
            return ((T)view);
        }
        return (null);
    }
}

// EOF
