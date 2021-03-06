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

public class Heatmap extends Trace
{
    public String type = "heatmap";
    public Boolean showlegend;
    public Object[] x;
    public Object[] y;
    public Double opacity;
    public XAxis xaxis = new XAxis ();
    public YAxis yaxis = new YAxis ();
    public String[] text;
    public Boolean connectgaps;
    public String[] colorscale;
    public Boolean reversescale;
    public Boolean showscale;
    public Boolean zauto;
    public Integer zmax;
    public Integer zmin;
    public Object[] z;
    public Integer dx;
    public Integer dy;
    public String ytype;
    public Integer x0;
    public String xtype;
    public Integer y0;
    public String zsmooth; // false/best/fast
    public Colorbar colorbar = new Colorbar ();
}

// EOF
