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

import java.util.List;
import java.util.ArrayList;

public class Layout
{
    public String title;
    public Font titlefont = new Font ();
    public Font font = new Font ();
    public Boolean showlegend;
    public Boolean autosize;
    public Integer width;
    public Integer height;
    public XAxis xaxis = new XAxis ();
    public YAxis yaxis = new YAxis ();
    public Legend legend = new Legend ();
    public List<Annotation> annotation = new ArrayList<Annotation> ();
    public Margin margin = new Margin ();
    public String paper_bgcolor;
    public String plot_bgcolor;
    public String hovermode; // closest/x/y
    public String dragmode; // zoom/pan/rotate
    public String separators;
    public String barmode;
    public Integer bargap;
    public Integer bargroupgap;
    public String barnorm;
    public String boxmode;
    public Integer boxgap;
    public Integer boxgapgroup;
    public RadialAxis radialaxis = new RadialAxis ();
    public AngularAxis angularaxis = new AngularAxis ();
    public Scene scene = new Scene ();
    public String direction;
    public Integer orientation;
    public Boolean hidesources;
}

// EOF
