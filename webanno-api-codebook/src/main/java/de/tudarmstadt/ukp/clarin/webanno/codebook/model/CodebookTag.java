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
package de.tudarmstadt.ukp.clarin.webanno.codebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * A persistence object for a CodebookTag
 */
@Entity
@Table(name = "codebook_tag")
public class CodebookTag
        implements Serializable
{

    private static final long serialVersionUID = -365458413044960909L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(length = 64000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "codebook_category")
    private CodebookCategory category;

    public CodebookTag()
    {
        // Nothing to do
    }

    public CodebookTag(CodebookCategory aCategory, String aName)
    {
        category = aCategory;
        name = aName;
    }

    public CodebookTag(String aName, String aDescription)
    {
        name = aName;
        description = aDescription;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long aId)
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

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    public CodebookCategory getCategory()
    {
        return category;
    }

    public void setCategory(CodebookCategory aCategory)
    {
        category = aCategory;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((category == null) ? 0 : category.hashCode());
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
        CodebookTag other = (CodebookTag) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (category == null) {
            if (other.category != null) {
                return false;
            }
        }
        else if (!category.equals(other.category)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return name;
    }

    // BEGIN HACK
    @Transient
    private boolean reordered;

    public void setReordered(boolean aB)
    {
        reordered = aB;
    }

    public boolean getReordered()
    {
        return reordered;
    }
    // END HACK
}
