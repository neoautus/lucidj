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

var lucidj_vaadin_helper = lucidj_vaadin_helper || {};

lucidj_vaadin_helper.reloadStyleSheet = function (url)
{
    for (var i = 0; i < document.styleSheets.length; i++)
    {
        var short_ref = document.styleSheets [i].href;

        if (short_ref != null)
        {
            short_ref = short_ref.substring(short_ref.indexOf('/', 8));

            if (short_ref == url || short_ref.substring(0, short_ref.indexOf('?')) == url)
            {
                var full_href = document.styleSheets [i].href;

                // Cool snippet from: https://github.com/NV/css_auto-reload ()
                var t = full_href.indexOf('?'),
                    last_reload = 'last_reload=' + (new Date).getTime();
                if (t < 0) {
                    full_href += '?' + last_reload;
                } else if (full_href.indexOf('last_reload=', t) < 0) {
                    full_href += '&' + last_reload;
                } else {
                    full_href = full_href.replace(/last_reload=\d+/, last_reload);
                }
                document.styleSheets [i].ownerNode.href = full_href;
                //vaadin.forceLayout ();  needed?
                break;
            }
        }
    }
};

lucidj_vaadin_helper.clearUrl = function ()
{
    var url = window.location.href
        .replace(/\btoken=[^\s&#]*/, '')
        .replace(/\?#/, '#')
        .replace(/\?&/, '?')
        .replace(/&&/, '&')
        .replace(/&$/, '');
    window.history.replaceState(window.history.state, document.title, url);
}

// EOF
