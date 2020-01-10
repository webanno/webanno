/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt  
 *  and Language Technology Group  Universität Hamburg 
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.export;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * All required contents of a {@link de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook}
 * project to be exported.
 *
 *
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedCodebook
{
    @JsonProperty("name")
    private java.lang.String name;

    @JsonProperty("uiName")
    private java.lang.String uiName;

    @JsonProperty("description")
    private java.lang.String description;

    @JsonProperty("project_name")
    private java.lang.String projectName;

    @JsonProperty("order")
    private int codebookOrder;

    @JsonProperty("features")
    private List<ExportedCodebookFeature> features;

    @JsonProperty("tags")
    private List<ExportedCodebookTag> tags;

    @JsonProperty("parent")
    private ExportedCodebook parent;

    public java.lang.String getName()
    {
        return name;
    }

    public void setName(java.lang.String name)
    {
        this.name = name;
    }

    public List<ExportedCodebookFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<ExportedCodebookFeature> features)
    {
        this.features = features;
    }

    public List<ExportedCodebookTag> getTags()
    {
        return tags;
    }

    public void setTags(List<ExportedCodebookTag> tags)
    {
        this.tags = tags;
    }

    public java.lang.String getUiName()
    {
        return uiName;
    }

    public void setUiName(java.lang.String uiName)
    {
        this.uiName = uiName;
    }

    public java.lang.String getDescription()
    {
        return description;
    }

    public void setDescription(java.lang.String description)
    {
        this.description = description;
    }

    public java.lang.String isProjectName()
    {
        return projectName;
    }

    public void setProjectName(java.lang.String projectName)
    {
        this.projectName = projectName;
    }

    public int getOrder()
    {
        return codebookOrder;
    }

    public void setOrder(int codebookOrder)
    {
        this.codebookOrder = codebookOrder;
    }

    public ExportedCodebook getParent()
    {
        return parent;
    }

    public void setParent(ExportedCodebook parent)
    {
        this.parent = parent;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(name).append(projectName).append(parent)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        ExportedCodebook that = (ExportedCodebook) o;

        return new EqualsBuilder().append(name, that.name).append(features, that.features)
                .append(tags, that.tags).append(description, that.description)
                .append(parent, that.parent).isEquals();
    }
}
