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

org_rationalq_aceeditor_AceEditor = function ()
{
    var self = this;
    var state = this.getState();
    var el = this.getElement();
    var last_height;
    var timer_afterRender;
    var timer_afterRender_ms = 300;
    var timer_change;
    var timer_change_ms = 200;
    var timer_focus;
    var timer_focus_ms = 50;
    var timer_blur;
    var timer_blur_ms = 50;

    var langTools = ace.require("ace/ext/language_tools");
    var editor = new ace.edit(el);

    // Wrap control MUST come before contents and other settings...
    editor.session.setWrapLimitRange(null, null);
    editor.session.setUseWrapMode(true);

    var sample_library = {
        getCompletions: function(editor, session, pos, prefix, callback) {
            comps = [
                {"name":"Vaadin","value":"Vaadin","score":300,"meta":"Library"},
                {"name":"Console","value":"Console","score":300,"meta":"Library"},
                {"name":"Pipe","value":"Pipe","score":300,"meta":"Library"}];
            callback (null, comps);
        }
    };

    // This should be BEFORE enableBasicAutocompletion
    langTools.setCompleters ([sample_library, langTools.textCompleter, langTools.keyWordCompleter]);

    // Editor contents MUST come before all other configurations...
    editor.setOptions
    ({
        maxLines: 299792458, // Don't code too fast...
        enableBasicAutocompletion: false,
        enableLiveAutocompletion: false
    });
    editor.setValue(state.s_value, -1);
    editor.moveCursorTo(state.s_cursor_row, state.s_cursor_column);
    console.debug("state.cursor_row=" + state.s_cursor_row + ", state.cursor_column=" + state.s_cursor_column);

    // ... All other settings follow
    editor.setTheme("ace/theme/chrome");
    editor.setAutoScrollEditorIntoView(true);
    editor.setDisplayIndentGuides(true);
    editor.setHighlightActiveLine(false);

    if (state.s_mode != null)
    {
        editor.session.setMode(state.s_mode);
    }

    // Remove Ctrl-Up and Ctrl-Down
    delete editor.commands.commandKeyBinding["ctrl-up"];
    delete editor.commands.commandKeyBinding["ctrl-down"];

    this.deferred_on_afterRender = function()
    {
        console.debug("deferred_on_afterRender");
        self.server_afterRender (el.offsetWidth, el.offsetHeight);
    };

    // Init last_height with size sent from server
    last_height = parseInt(state.height);

    // TODO: CHECK *WHY* AND *HOW* vaadin.forceLayout() WORKS
    editor.renderer.on("afterRender", function()
    {
        if (last_height != el.offsetHeight)
        {
            last_height = el.offsetHeight;
            vaadin.forceLayout ();

            clearTimeout (timer_afterRender);
            timer_afterRender = setTimeout (self.deferred_on_afterRender, timer_afterRender_ms);
        }
    });

    // (!!!) Force resize/rendering at THIS POINT to avoid flickering
    editor.resize();

    this.deferred_on_change = function()
    {
        console.debug("deferred_on_change");
        self.server_onChangeValue(editor.getValue());
    };

    editor.on('change', function(delta)
    {
        console.trace(delta);
        clearTimeout (timer_change);
        timer_change = setTimeout (self.deferred_on_change, timer_change_ms);
    });

    this.deferred_on_focus = function()
    {
        self.server_onFocus();
    };

    editor.on('focus', function()
    {
        clearTimeout (timer_focus);
        timer_focus = setTimeout (self.deferred_on_focus, timer_focus_ms);
    });

    this.deferred_on_blur = function()
    {
        self.server_onBlur(editor.getCursorPosition ());
    };

    editor.on('blur', function()
    {
        clearTimeout (timer_blur);
        timer_blur = setTimeout (self.deferred_on_blur, timer_blur_ms);
    });

    this.client_setValue = function(value)
    {
        console.debug("this.client_setValue");

        // Set editor text
        editor.setValue(value, -1);

        // TODO: REALLY NEEDED HERE?
        editor.resize();
    };

    this.client_focus = function()
    {
        editor.focus();
    };

    this.client_setMode = function(mode)
    {
        editor.session.setMode(mode);
    };

    //-------------------------
    // Vaadin state management
    //-------------------------

    this.onStateChange = function()
    {
        // TODO: CHECK IF INDIVIDUAL OPTIONS SHOULD BE SET
        // Import current options
        editor.setOptions(JSON.parse (this.getState ().s_options));
    };
};

// EOF
