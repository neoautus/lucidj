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

package org.lucidj.aceeditor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.event.FieldEvents;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.Component;
import com.vaadin.ui.JavaScriptFunction;

import java.util.ArrayList;
import java.util.HashMap;

@JavaScript ({ "vaadin://~/aceeditor_libraries/ace-builds/src-noconflict/ace.js", "vaadin://~/aceeditor_libraries/ace-builds/src-noconflict/ext-language_tools.js", "aceeditor.js" })
@StyleSheet ("vaadin://~/aceeditor_libraries/aceeditor.css")
public class AceEditor extends AbstractJavaScriptComponent implements Component.Focusable
{
    private final static Logger log = LoggerFactory.getLogger (AceEditor.class);

    private transient Gson gson = new Gson ();
    private transient Gson pretty = new GsonBuilder().setPrettyPrinting().create();

    private HashMap c_options = new HashMap ();
    private String c_value = "";
    private String c_mode = null;

    private boolean height_undefined = false;
    private boolean width_undefined = false;
    private int c_cursor_row = 0;
    private int c_cursor_column = 0;
    private int tab_index = 0;

    private void sync_state (boolean markAsDirty)
    {
        final AceEditorState state = getState (markAsDirty);

        state.s_options = gson.toJson (c_options);
        state.s_value = c_value;
        state.s_cursor_row = c_cursor_row;
        state.s_cursor_column = c_cursor_column;
        state.s_mode = c_mode;
    }

    @Override
    public void attach ()
    {
        super.attach ();
        sync_state (false);
    }

    public AceEditor ()
    {
        final AceEditorState state = getState (false);
        final AceEditor self = this;

        sync_state (false);

        addStyleName ("aceeditor");

        addFunction("server_afterRender", new JavaScriptFunction()
        {
            @Override
            public void call(JsonArray arguments)
            {
                log.debug ("server_afterRender: arguments = {}", arguments.toJson());

                // When we receive afterRender event, the editor contents are already loaded.
                // We only use s_value to do the _initial load_ for the js component.
                // Afterwards we _only_ sync editor contents using client_setValue js call.
                // So we are safe to clear s_value now and save some byte traffic.
                //state.s_value = "";   --- PAGE REFRESH BUG (EMPTY ACE EDITOR)

                if (getWidth () == -1)
                {
                    width_undefined = true;
                }

                if (width_undefined)
                {
                    String width = arguments.get(0).asString () + "px";
                    setWidth (width);
                    state.width = width;
                    log.debug ("setWidth = {}", (float)arguments.get(0).asNumber ());
                }

                if (getHeight() == -1)
                {
                    height_undefined = true;
                }

                if (height_undefined)
                {
                    String height = arguments.get(1).asString () + "px";

                    setHeight (height);
                    state.height = height;
                    log.debug ("setHeight = {}", (float)arguments.get(1).asNumber ());
                }
            }
        });

        addFunction("server_onChangeValue", new JavaScriptFunction()
        {
            @Override
            public void call(JsonArray arguments)
            {
                log.debug ("server_onChangeValue: arguments = {}", arguments.toJson());

                c_value = arguments.getString(0);

                for (FieldEvents.TextChangeListener listener: textChangeListeners)
                {

                    listener.textChange (new AceEditorTextChangeEvent (self));
                }
            }
        });

        addFunction("server_onFocus", new JavaScriptFunction()
        {
            @Override
            public void call(JsonArray arguments)
            {
                log.debug ("server_onFocus: arguments = {}", arguments.toJson());

                for (FieldEvents.FocusListener listener: focusListeners)
                {
                    listener.focus (new FieldEvents.FocusEvent (self));
                }
            }
        });

        addFunction("server_onBlur", new JavaScriptFunction()
        {
            @Override
            public void call(JsonArray arguments)
            {
                log.debug ("server_onBlur: arguments = {}", arguments.toJson());

                JsonObject cursor = arguments.getObject (0);

                if (cursor.hasKey ("row"))
                {
                    c_cursor_row = (int)arguments.getObject (0).getNumber ("row");
                    c_cursor_column = (int)arguments.getObject (0).getNumber("column");
                    log.debug ("server_onBlur: row={} column={}", c_cursor_row, c_cursor_column);
                }
            }
        });
    }

    @Override
    protected AceEditorState getState (boolean markAsDirty)
    {
        return ((AceEditorState)super.getState (markAsDirty));
    }

    @Override
    protected AceEditorState getState ()
    {
        return ((AceEditorState)super.getState (true));
    }

    @Override
    public void setVisible (boolean visible)
    {
        super.setVisible (visible);

        // We don't always keep state in sync, so we need to set s_value here
        getState (false).s_value = c_value;
    }

    @SuppressWarnings("unchecked")
    private void set_option (String key, Object value)
    {
        HashMap attr_map = c_options;

        if (key.contains (":"))
        {
            String[] keys = key.split (":");

            for (int i = 0; i < keys.length - 1; i++)
            {
                HashMap next_map = (HashMap)attr_map.get (keys [i]);

                if (next_map == null)
                {
                    next_map = new HashMap ();
                    attr_map.put (keys [i], next_map);
                }

                attr_map = next_map;
            }

            key = keys [keys.length - 1];
        }

        attr_map.put (key, value);

        getState().s_options = gson.toJson (c_options);
        markAsDirty();
    }

    public String getPrettyJson ()
    {
        return (pretty.toJson (c_options));
    }

    public void setMode (String mode)
    {
        c_mode = mode;
        getState (false).s_mode = c_mode;
        callFunction ("client_setMode", c_mode);
    }

    public void setOption (String key, boolean value)
    {
        set_option (key, value);
    }

    public void setOption (String key, int value)
    {
        set_option (key, value);
    }

    public void setOption (String key, String value)
    {
        set_option (key, value);
    }

    public void setOption (String key, Object[] value)
    {
        set_option (key, value);
    }

    //========================================================================
    // FOCUS
    //========================================================================

    ArrayList<FieldEvents.FocusListener> focusListeners = new ArrayList<>();

    public void addFocusListener (FieldEvents.FocusListener listener)
    {
        focusListeners.add (listener);
    }

    @Override
    public void focus ()
    {
        // The call 'super.focus()' generates the following message on browser side:
        //  SEVERE: Server is trying to set focus to the widget of connector
        //  JavaScriptComponentConnector (111) but it is not focusable. The widget
        //  should implement either com.google.gwt.user.client.ui.Focusable or
        //  com.vaadin.client.Focusable
        //super.focus();
        callFunction ("client_focus");
    }

    @Override
    public int getTabIndex ()
    {
        return (tab_index);
    }

    @Override
    public void setTabIndex (int i)
    {
        tab_index = i;
    }

    //========================================================================
    // VALUE
    //========================================================================

    ArrayList<FieldEvents.TextChangeListener> textChangeListeners = new ArrayList<>();

    public void addTextChangeListener (FieldEvents.TextChangeListener listener)
    {
        textChangeListeners.add (listener);
    }

    public static class AceEditorTextChangeEvent extends FieldEvents.TextChangeEvent
    {
        private String curText;
        private int cursorPosition;

        private AceEditorTextChangeEvent(final AceEditor ace)
        {
            super(ace);
            curText = ace.getValue();
            cursorPosition = 0; //ace.getCursorPosition();
        }

        @Override
        public Component getComponent()
        {
            return (super.getComponent());
        }

        @Override
        public String getText()
        {
            return curText;
        }

        @Override
        public int getCursorPosition()
        {
            return cursorPosition;
        }

    }

    public String getValue ()
    {
        return (c_value);
    }

    public void setValue (String value)
    {
        c_value = value;

        // Only push changes if we are attached
        if (isAttached ())
        {
            callFunction ("client_setValue", c_value);
        }
    }
}

// EOF
