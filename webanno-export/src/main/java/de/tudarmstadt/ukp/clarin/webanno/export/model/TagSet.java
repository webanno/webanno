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
package de.tudarmstadt.ukp.clarin.webanno.export.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * All required contents of a tagset to be exported. The tagsets to be exported are those
 * created for a project, hence project specific.
 *
 */
@JsonPropertyOrder(value = { "name", "typeUiName","description", "language", "type", "typeName",
        "typeDescription" ,"tags" })
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagSet
{
    @JsonProperty("name")
    String name;

    // back compatibility
    @JsonProperty("typeUiName")
    String typeUiName;

    @JsonProperty("description")
    String description;

    @JsonProperty("language")
    String language;

 // back compatibility
    @JsonProperty("type")
    String type;

 // back compatibility
    @JsonProperty("type_name")
    String typeName;

 // back compatibility
    @JsonProperty("type_description")
    String typeDescription;

    @JsonProperty("tags")
    List<Tag> tags = new ArrayList<>();

    @JsonProperty("create_tag")
    private boolean createTag;
    public String getName()
    {
        return name;
    }
    public void setName(String aName)
    {
        name = aName;
    }
    public String getDescription()
    {
        return description;
    }
    public void setDescription(String aDescription)
    {
        description = aDescription;
    }
    public String getLanguage()
    {
        return language;
    }
    public void setLanguage(String aLanguage)
    {
        language = aLanguage;
    }
    public String getType()
    {
        return type;
    }
    public String getTypeName()
    {
        return typeName;
    }
    public void setTypeName(String aTypeName)
    {
        typeName = aTypeName;
    }
    public String getTypeDescription()
    {
        return typeDescription;
    }
    public void setTypeDescription(String aTypeDescription)
    {
        typeDescription = aTypeDescription;
    }
    public List<Tag> getTags()
    {
        return tags;
    }
    public void setTags(List<Tag> aTags)
    {
        tags = aTags;
    }
    public String getTypeUiName()
    {
        return typeUiName;
    }
    public void setTypeUiName(String typeUiName)
    {
        this.typeUiName = typeUiName;
    }
    public boolean isCreateTag()
    {
        return createTag;
    }
    public void setCreateTag(boolean createTag)
    {
        this.createTag = createTag;
    }

}
