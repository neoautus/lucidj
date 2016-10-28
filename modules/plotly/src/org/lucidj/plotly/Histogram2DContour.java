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

public class Histogram2DContour extends Trace
{
    public String type = "histogram2dcontour";
    public Boolean showlegend;
    public Object[] x;
    public Object[] y;
    public Double opacity;
    public XAxis xaxis = new XAxis ();
    public YAxis yaxis = new YAxis ();
    public String xsrc;
    public String ysrc;
    public Line line = new Line ();
    public Boolean autobinx;
    public Boolean autobiny;
    public String histfunc; // ''/percent/probability/density/probability density;
    public String histnorm; // ''/count/sum/avg/min/max;
    public Integer nbinsx;
    public Integer nbinsy;
    public Object xbins = new XBins ();
    public Object ybins = new YBins ();
    public String[] colorscale;
    public Boolean reversescale;
    public Boolean showscale;
    public Boolean zauto;
    public Integer zmax;
    public Integer zmin;
    public Colorbar colorbar = new Colorbar ();
    public Boolean autocontour;
    public Contours contours = new Contours ();
    public Integer ncontours;
}

// EOF
