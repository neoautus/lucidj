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

xyz_kuori_dygraphs_Dygraphs = function ()
{
    var e = this.getElement();

    this.onStateChange = function()
    {
        var file = JSON.parse (this.getState ().file);
        var attrs = JSON.parse (this.getState ().attrs);

        function patch_references (obj)
        {
            for (var k in obj)
            {
                if (typeof obj[k] == "object" && obj[k] !== null)
                {
                    patch_references (obj[k]);
                }
                else if (typeof obj[k] === 'string' && obj[k].substring (0, 1) === '@')
                {
                    obj[k] = eval ("obj[k] = (" + obj[k].substring (1) + ")");
                }
            }
        }

        patch_references (attrs);

        if (!e.hasOwnProperty ("dygraphs"))
        {
            e.dygraphs = new Dygraph (e, file, attrs);
            //e.style.background = 'yellow';

            this.addResizeListener (e, function (ev)
            {
                ev.element.dygraphs.resize ();
            });
        }
        else
        {
            if (this.getState ().file !== '')
            {
                attrs.file = file;
            }

            e.dygraphs.updateOptions (attrs);
        }
    }
}

// EOF
