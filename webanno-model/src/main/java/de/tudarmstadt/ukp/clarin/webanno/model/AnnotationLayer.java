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
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.ForeignKey;


/**
 * A persistence object for an annotation layer. Currently, the builtin layers are:
 * {@literal
 *  'pos' as  'span',
 *  'dependency' as 'relation',
 *   'named entity' as 'span',
 *   'coreference type' as 'chain', and
 *   'coreference' as 'chain'
 *  }
 *
 *
 */
@Entity
@Table(name = "annotation_type", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "project" }) })
public class AnnotationLayer
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private long id;

    @Column(nullable = false)
    private String uiName;

    @Column(nullable = false)
    private String type;

    @Lob
    @Column(length = 64000)
    private String description;

    private boolean enabled = true;

    private boolean builtIn = false;
    
    private boolean readonly = false;
    
    private boolean isZeroWidthOnly = false;
    
    private String onClickJavaScriptAction;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
   // @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    @ForeignKey(name = "none")
    @JoinColumn(name = "annotation_type")
    private AnnotationLayer attachType;

    @ManyToOne
    @ForeignKey(name = "none")
    @JoinColumn(name = "annotation_feature")
    private AnnotationFeature attachFeature;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    private boolean lockToTokenOffset = true;

    // There wase a type in the code which unfortunately made it into databases...
    @Column(name="allowSTacking")
    private boolean allowStacking;

    private boolean crossSentence;

    private boolean multipleTokens;
    
    private boolean linkedListBehavior;

    
    public AnnotationLayer()
    {
        // Required
    }
    
    public AnnotationLayer(String aName, String aUiName, String aType, Project aProject, boolean aBuiltIn)
    {
        setName(aName);
        setUiName(aUiName);
        setProject(aProject);
        setBuiltIn(aBuiltIn);
        setType(aType);
    }
    
    /**
     * A short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     * 
     * @return the id.
     */
    public long getId()
    {
        return id;
    }

    /**
     * A short unique numeric identifier for the type (primary key in the DB). This identifier is
     * only transiently used when communicating with the UI. It is not persisted long term other
     * than in the type registry (e.g. in the database).
     * 
     * @param typeId the id.
     */
    public void setId(long typeId)
    {
        this.id = typeId;
    }

    /**
     * The type of the annotation, either span, relation or chain
     * 
     * @return the type.
     */
    public String getType()
    {
        return type;
    }

    /**
     * The type of the annotation, either span, relation or chain
     * 
     * @param aType the type.
     */
    public void setType(String aType)
    {
        type = aType;
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
     * @param uiName the displayed name.
     */
    public void setUiName(String uiName)
    {
        this.uiName = uiName;
    }

    /**
     * Whether the type is available in the UI (outside of the project settings).
     * 
     * @return whether the type is enabled.
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Whether the type is available in the UI (outside of the project settings).
     * 
     * @param enabled if the type is enabled.
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     * 
     * @return if the type is built-in.
     */
    public boolean isBuiltIn()
    {
        return builtIn;
    }

    /**
     * Whether annotations of this type can be deleted. E.g. WebAnno currently does not support
     * deleting Lemma annotations. This is always “false” for user-created types.
     * 
     * @param builtIn if the type is built-in.
     */
    public void setBuiltIn(boolean builtIn)
    {
        this.builtIn = builtIn;
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
     * @param annotationTypeName the type name.
     */
    public void setName(String annotationTypeName)
    {
        this.name = annotationTypeName;
    }

    /**
     * if an annotation type cannot exist alone, this determines the type of an annotation to which
     * it must be attached. If an attachType is set, an annotation cannot be created unless an
     * attachType annotation is present before. If a attachType annotation is deleted, all
     * annotations attached to it must be located and deleted as well. E.g. a POS annotation must
     * always be attached to a Token annotation. A Dependency annotation must always be attached to
     * two Tokens (the governor and the dependent). This is handled differently for spans and arcs
     * 
     * @return the attach type name.
     */
    public AnnotationLayer getAttachType()
    {
        return attachType;
    }

    /**
     * if an annotation type cannot exist alone, this determines the type of an annotation to which
     * it must be attached. If an attachType is set, an annotation cannot be created unless an
     * attachType annotation is present before. If a attachType annotation is deleted, all
     * annotations attached to it must be located and deleted as well. E.g. a POS annotation must
     * always be attached to a Token annotation. A Dependency annotation must always be attached to
     * two Tokens (the governor and the dependent). This is handled differently for spans and arcs
     * 
     * @param attachType the attach type name.
     */

    public void setAttachType(AnnotationLayer attachType)
    {
        this.attachType = attachType;
    }

    /**
     * used if the attachType does not provide sufficient information about where to attach an
     * annotation
     *
     * @return the attach feature.
     */
    public AnnotationFeature getAttachFeature()
    {
        return attachFeature;
    }

    /**
     * used if the attachType does not provide sufficient information about where to attach an
     * annotation
     * 
     * @param attachFeature the attach feature.
     */
    public void setAttachFeature(AnnotationFeature attachFeature)
    {
        this.attachFeature = attachFeature;
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
     * @param project the project.
     */
    public void setProject(Project project)
    {
        this.project = project;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
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
        AnnotationLayer other = (AnnotationLayer) obj;
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

    public boolean isLockToTokenOffset()
    {
        return lockToTokenOffset;
    }

    public void setLockToTokenOffset(boolean lockToTokenOffset)
    {
        this.lockToTokenOffset = lockToTokenOffset;
    }

    public boolean isAllowStacking()
    {
        return allowStacking;
    }

    public void setAllowStacking(boolean allowStacking)
    {
        this.allowStacking = allowStacking;
    }

    public boolean isCrossSentence()
    {
        return crossSentence;
    }

    public void setCrossSentence(boolean crossSentence)
    {
        this.crossSentence = crossSentence;
    }

    public boolean isMultipleTokens()
    {
        return multipleTokens;
    }

    public void setMultipleTokens(boolean multipleTokens)
    {
        this.multipleTokens = multipleTokens;
    }

    public boolean isLinkedListBehavior()
    {
        return linkedListBehavior;
    }

    public void setLinkedListBehavior(boolean aLinkedListBehavior)
    {
        linkedListBehavior = aLinkedListBehavior;
    }

    public boolean isReadonly()
    {
        return readonly;
    }

    public void setReadonly(boolean aReadonly)
    {
        readonly = aReadonly;
    }

	public boolean isZeroWidthOnly() {
		return isZeroWidthOnly;
	}

	public void setZeroWidthOnly(boolean isZeroWidthOnly) {
		this.isZeroWidthOnly = isZeroWidthOnly;
	}

	public String getOnClickJavaScriptAction() {
		return onClickJavaScriptAction;
	}

	public void setOnClickAction(String onClickAction) {
		this.onClickJavaScriptAction = onClickAction;
	}
}
