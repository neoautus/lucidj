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

package org.lucidj.plotly;

public class Bar extends Trace
{
    public String type = "bar";
    public Boolean showlegend;
    public Object[] x;
    public Object[] y;
    public Double opacity;
    public XAxis xaxis = new XAxis ();
    public YAxis yaxis = new YAxis ();
    public String xsrc;
    public String ysrc;
    public Marker marker = new Marker ();
    public String[] text;
    public Error_X error_x = new Error_X ();
    public Error_Y error_y = new Error_Y ();
    public Integer[] r;
    public Object[] t;
    public String orientation; // h/v
}

// EOF
