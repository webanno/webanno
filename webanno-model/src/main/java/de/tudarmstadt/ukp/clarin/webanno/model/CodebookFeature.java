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
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

/**
 * A feature for Codebook annotation type
 */
@Entity
@Table(name = "codebook_feature", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "codebook", "name", "project" }) })
public class CodebookFeature implements Serializable {
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String type;

    @ManyToOne
    @JoinColumn(name = "codebook", 
        foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private Codebook codebook;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;


    @ManyToOne
    @JoinColumn(name = "tag_set", 
        foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private TagSet tagset;

    @Column(nullable = false)
    private String uiName;

    @Lob
    @Column(length = 64000)
    private String description;

    @Column(nullable = false)
    private String name;


    public CodebookFeature() {
        // Nothing to do
    }

    // Visible for testing
    public CodebookFeature(String aName, String aType) {
        name = aName;
        uiName = aName;
        type = aType;
    }

    public CodebookFeature(Project aProject, Codebook aCodebook, String aName, String aUiName,
            String aType) {
        project = aProject;
        codebook = aCodebook;
        name = aName;
        uiName = aUiName;
        type = aType;
    }

    public CodebookFeature(Project aProject,  Codebook aCodebook, String aName, String aUiName,
            String aType, String aDescription, TagSet aTagSet) {
        project = aProject;
        codebook = aCodebook;
        name = aName;
        uiName = aUiName;
        type = aType;
        description = aDescription;
        tagset = aTagSet;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Get the type of feature (string, integer, float, boolean, or a span type used
     * as a label)
     * 
     * @return the type of feature.
     */
    public String getType() {
        return type;
    }

    public Codebook getCodebook() {
        return codebook;
    }

    public void setCodebook(Codebook codebook) {
        this.codebook = codebook;
    }

    /**
     * The type of feature (string, integer, float, boolean, or a span type used as
     * a label)
     * 
     * @param type
     *            the type of feature.
     */
    public void setType(String type) {
        this.type = type;
    }



    public Project getProject() {
        return project;
    }

    /**
     * @param project
     *            the project.
     */
    public void setProject(Project project) {
        this.project = project;
    }


    /**
     * The name of the feature as displayed in the UI.
     * 
     * @return the name displayed in the UI.
     */
    public String getUiName() {
        return uiName;
    }

    /**
     * The name of the feature as displayed in the UI.
     * 
     * @param uiName
     *            the name displayed in the UI.
     */
    public void setUiName(String uiName) {
        this.uiName = uiName;
    }

    /**
     * A description of the feature.
     * 
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * A description of the feature.
     * 
     * @param description
     *            the description.
     */
    public void setDescription(String description) {
        this.description = description;
    }



    /**
     * The name of the feature in the UIMA type system.
     *
     * @return the UIMA type name.
     */
    public String getName() {
        return name;
    }

    /**
     * The name of the feature in the UIMA type system.
     * 
     * @param name
     *            the UIMA type name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the tagset.
     */
    public TagSet getTagset() {
        return tagset;
    }

    /**
     * @param tagset
     *            the tagset.
     */
    public void setTagset(TagSet tagset) {
        this.tagset = tagset;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("name", name).build();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((codebook == null) ? 0 : codebook.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        CodebookFeature other = (CodebookFeature) obj;
        if (codebook == null) {
            if (other.codebook != null) {
                return false;
            }
        } else if (!codebook.equals(other.codebook)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (project == null) {
            if (other.project != null) {
                return false;
            }
        } else if (!project.equals(other.project)) {
            return false;
        }
        if (type == null) {
            if (other.type != null) {
                return false;
            }
        } else if (!type.equals(other.type)) {
            return false;
        }
        return true;
    }
}
