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

package org.rationalq.aceeditor;

import com.vaadin.shared.ui.JavaScriptComponentState;

public class AceEditorState extends JavaScriptComponentState
{
    public String s_value;
    public String s_options;
    public String s_mode;
    public int s_cursor_row;
    public int s_cursor_column;
}

// EOF
