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
package de.tudarmstadt.ukp.clarin.webanno.export.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * All required contents of a
 * {@link de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook} project to be
 * exported.
 *
 *
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportedCodebook
{
    @JsonProperty("name")
    String name;

    @JsonProperty("features")
    private List<ExportedCodebookFeature> features;

    @JsonProperty("uiName")
    private String uiName;

    @JsonProperty("description")
    private String description;
    
    @JsonProperty("order")
    int codebookOrder;

    @JsonProperty("project_name")
    private String projectName;
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
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

    public String getUiName()
    {
        return uiName;
    }

    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String isProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }
    
    public int getOrder() {
        return codebookOrder;
    }

    public void setOrder(int codebookOrder) {
        this.codebookOrder = codebookOrder;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
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
        ExportedCodebook other = (ExportedCodebook) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (projectName != other.projectName) {
            return false;
        }
        return true;
    }
}
