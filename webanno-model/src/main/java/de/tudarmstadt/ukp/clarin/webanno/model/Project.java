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
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;

/**
 * A persistence object for a Project.
 */
@Entity
@Table(name = "project", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) })
public class Project
    implements Serializable
{
    private static final long serialVersionUID = -5426914078691460011L;

    @Id
    @GeneratedValue
    private long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(length = 64000)
    private String description;

    private String mode;

    // version of the project
    private int version = 1;
    
    // Disable users from exporting annotation documents
    private boolean disableExport = false;
    
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.ScriptDirectionType")
    private ScriptDirection scriptDirection;

    public Project()
    {
        // Nothing to do
    }

    public long getId()
    {
        return id;
    }

    public void setId(long aId)
    {
        id = aId;
    }

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

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public boolean isDisableExport()
    {
        return disableExport;
    }

    public void setDisableExport(boolean disableExport)
    {
        this.disableExport = disableExport;
    }

    public ScriptDirection getScriptDirection()
    {
        // If unset, default to LTR - property was not present in older WebAnno versions
        if (scriptDirection == null) {
            return ScriptDirection.LTR;
        }
        else {
            return scriptDirection;
        }
    }

    public void setScriptDirection(ScriptDirection scriptDirection)
    {
        this.scriptDirection = scriptDirection;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Project other = (Project) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public String getMode()
    {
        return mode;
    }

    public void setMode(String aMode)
    {
        this.mode = aMode;
    }

}
