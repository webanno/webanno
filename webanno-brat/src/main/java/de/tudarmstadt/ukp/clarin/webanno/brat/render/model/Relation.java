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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.BeanAsArraySerializer;

/**
 * A relation between span annotations -&gt; an arc annotation. Example
 * "relations":[["d_48420","SUBJ",[["Arg1","p_21346"],["Arg2","p_21341"]]],...
 *
 *
 */
@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "vid", "type", "arguments", "labelText", "color" })
public class Relation
{
    private VID vid;

    /**
     * The type of the relation between two spans
     */
    private String type;

    /**
     * The initial/destination span annotations as shown in the example above
     */
    private List<Argument> arguments = new ArrayList<>();

    // WEBANNO EXTENSION BEGIN
    private String labelText;
    private String color;
    // WEBANNO EXTENSION END
    
    public Relation()
    {
        // Nothing to do
    }

    public Relation(int aId, String aType, List<Argument> aArguments, String aLabelText,
            String aColor)
    {
        this(new VID(aId), aType, aArguments, aLabelText, aColor);
    }

    public Relation(VID aVid, String aType, List<Argument> aArguments, String aLabelText,
            String aColor)
    {
        vid = aVid;
        type = aType;
        arguments = aArguments;
        labelText = aLabelText;
        color = aColor;
    }

    @Deprecated
    public int getId()
    {
        return vid.getId();
    }

    @Deprecated
    public void setId(int aId)
    {
        vid = new VID(aId);
    }
    
    public VID getVid()
    {
        return vid;
    }

    public void setVid(VID aVid)
    {
        vid = aVid;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }

    public List<Argument> getArguments()
    {
        return arguments;
    }

    public void setArguments(List<Argument> aArguments)
    {
        arguments = aArguments;
    }

    public void setLabelText(String aLabelText)
    {
        labelText = aLabelText;
    }
    
    public String getLabelText()
    {
        return labelText;
    }

    public String getColor()
    {
        return color;
    }

    public void setColor(String aColor)
    {
        color = aColor;
    }
}
