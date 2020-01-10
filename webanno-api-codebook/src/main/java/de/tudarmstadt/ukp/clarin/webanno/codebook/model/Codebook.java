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
package de.tudarmstadt.ukp.clarin.webanno.codebook.model;

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

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

@Entity
@Table(name = "codebook", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project" }) })
public class Codebook
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String uiName;

    @Lob
    @Column(length = 64000)
    private String description;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "project")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Project project;

    @ManyToOne
    @JoinColumn(name = "parent", foreignKey =
        @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @OnDelete(action = OnDeleteAction.CASCADE) // TODO do we really want cascading delete?!
    private Codebook parent;

    @Column(name = "codebookorder", nullable = false)
    private int codebookOrder = 0;

    @Column(name = "createTag", nullable = false)
    private boolean createTag = false;

    public Codebook()
    {
        // Required
    }

    public Codebook(String aName, String aUiName,
                    String aType, Project aProject, boolean aCreateTag)
    {
        setName(aName);
        setUiName(aUiName);
        setProject(aProject);
        setCreateTag(aCreateTag);
    }

    /**
     * A short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     *
     * @return the id.
     */
    public Long getId()
    {
        return id;
    }

    /**
     * A short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     *
     * @param typeId
     *            the id.
     */
    public void setId(Long typeId)
    {
        this.id = typeId;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String aDescription)
    {
        description = aDescription;
    }

    /**
     * The name displayed to the user in the UI.
     *
     * @return the displayed name.
     */
    public String getUiName()
    {
        return uiName;
    }

    /**
     * The name displayed to the user in the UI.
     *
     * @param uiName
     *            the displayed name.
     */
    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    /**
     * The name of the UIMA annotation type handled by the adapter. This name must be unique for
     * each type in a project
     *
     * @return the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * The name of the UIMA annotation type handled by the adapter. This name must be unique for
     * each type in a project
     *
     * @param annotationTypeName
     *            the type name.
     */
    public void setName(String annotationTypeName)
    {
        this.name = annotationTypeName;
    }

    /**
     * the project id where this type belongs to
     *
     * @return the project.
     */
    public Project getProject()
    {
        return project;
    }

    /**
     * the project id where this type belongs to
     *
     * @param project
     *            the project.
     */
    public void setProject(Project project)
    {
        this.project = project;
    }

    /**
     * @return The parent (if exist) of this codebook
     */
    public Codebook getParent()
    {
        return parent;
    }

    /**
     * Set the parent codebook of this codebook
     *
     * @param parent
     */
    public void setParent(Codebook parent)
    {
        this.parent = parent;
    }

    public boolean isCreateTag() {
        return createTag;
    }

    public void setCreateTag(boolean createTag) {
        this.createTag = createTag;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
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
        Codebook other = (Codebook) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        }
        else if (!name.equals(other.name)) {
            return false;
        }
        if (project == null) {
            if (other.project != null) {
                return false;
            }
        }
        else if (!project.equals(other.project)) {
            return false;
        }
        return true;
    }

    public int getOrder()
    {
        return codebookOrder;
    }

    public void setOrder(int order)
    {
        this.codebookOrder = order;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Codebook [name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
}
