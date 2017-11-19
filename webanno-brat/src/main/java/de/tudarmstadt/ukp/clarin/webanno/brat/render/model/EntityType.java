/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

import static de.tudarmstadt.ukp.clarin.webanno.brat.render.BratRenderer.abbreviate;
import static java.util.Arrays.asList;

import java.util.List;

/**
 * Different attributes of an Entity used for its visualisation formats. It looks like
 *
 * {"name":"Named Entity","type":"Named Entity","unused":true,"fgColor":"black","bgColor":"cyan",
 * "borderColor":"green","labels":[], "children":[{"name":"LOC","type":"LOC","unused":false,
 * "fgColor":"black","bgColor":"cyan","borderColor":"green","labels":["LOC"],"children":[],
 * "attributes":[],"arcs":[]},....
 *
 *
 */
public class EntityType
{
    private String name;
    private String type;
    private boolean unused;

    private String hotkey;

    private String fgColor;
    private String bgColor;
    private String borderColor;

    private List<String> labels;
    private List<EntityType> children;
    private List<String> attributes;

    private List<RelationType> arcs;

    public EntityType()
    {
        // Nothing to do
    }

    /**
     * @param aName
     *            the "name" (the UIMA type name).
     * @param aLabel
     *            the "label" which is displayed to the user e.g. in the comment pop-up (the display
     *            name)
     * @param aType
     *            the "type" (brat type name, which is basically an identifier used in the UI visual
     *            model)
     */
    public EntityType(String aName, String aLabel, String aType)
    {
        this(aName /* name */, aType /* type */, true /* unused */, "" /* hotkey */,
                null /* fgColor */, null /* bgColor */, null /* borderColor */,
                asList(aLabel, abbreviate(aLabel)) /* labels */, null /* children */,
                null /* attributes */, null /* arcs */);
    }
    
//    public EntityType(String aName, String aType, String aFgColor, String aBgColor,
//            String aBorderColor)
//    {
//        this(aName /* name */, aType /* type */, true /* unused */, "" /* hotkey */,
//                aFgColor /* fgColor */, aBgColor /* bgColor */, aBorderColor /* borderColor */,
//                Arrays.asList(aName) /* labels */, null /* children */, null /* attributes */, 
//                null /* arcs */);
//    }
    
    private EntityType(String aName, String aType, boolean aUnused, String aHotkey, String aFgColor,
            String aBgColor, String aBorderColor, List<String> aLabels, List<EntityType> aChildren,
            List<String> aAttributes, List<RelationType> aArcs)
    {
        super();
        name = aName;
        type = aType;
        unused = aUnused;
        hotkey = aHotkey;
        borderColor = aBorderColor;
        fgColor = aFgColor;
        bgColor = aBgColor;
        labels = aLabels;
        children = aChildren;
        attributes = aAttributes;
        arcs = aArcs;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public boolean isUnused()
    {
        return unused;
    }

    public void setUnused(boolean aUnused)
    {
        unused = aUnused;
    }

    public String getHotkey()
    {
        return hotkey;
    }

    public void setHotkey(String aHotkey)
    {
        hotkey = aHotkey;
    }

    public String getFgColor()
    {
        return fgColor;
    }

    public void setFgColor(String aFgColor)
    {
        fgColor = aFgColor;
    }

    public String getBgColor()
    {
        return bgColor;
    }

    public void setBgColor(String aBgColor)
    {
        bgColor = aBgColor;
    }

    public String getBorderColor()
    {
        return borderColor;
    }

    public void setBorderColor(String aBorderColor)
    {
        borderColor = aBorderColor;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public void setLabels(List<String> aLabels)
    {
        labels = aLabels;
    }

    public List<EntityType> getChildren()
    {
        return children;
    }

    public void setChildren(List<EntityType> aChildren)
    {
        children = aChildren;
    }

    public List<String> getAttributes()
    {
        return attributes;
    }

    public void setAttributes(List<String> aAttributes)
    {
        attributes = aAttributes;
    }

    public List<RelationType> getArcs()
    {
        return arcs;
    }

    public void setArcs(List<RelationType> aArcs)
    {
        arcs = aArcs;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EntityType other = (EntityType) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        }
        else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }

}
