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

import static java.util.Arrays.asList;

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

import org.hibernate.annotations.Type;

/**
 * A persistence object for an annotation layer. Currently, the builtin layers are:
 * {@literal
 *  'pos' as  'span',
 *  'dependency' as 'relation',
 *   'named entity' as 'span',
 *   'coreference type' as 'chain', and
 *   'coreference' as 'chain'
 *  }
 */
@Entity
@Table(name = "annotation_type", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "project" }) })
public class AnnotationLayer
    implements Serializable
{
    private static final long serialVersionUID = 8496087166198616020L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false)
    private String uiName;

    @Column(nullable = false)
    private String type;

    @Lob
    @Column(length = 64000)
    private String description;

    private boolean enabled = true;

    @Column(name = "builtIn")
    private boolean builtIn = false;
    
    private boolean readonly = false;
    
    @Lob
    @Column(nullable = true, length = 64000)
    private String onClickJavascriptAction;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
   // @ForeignKey(ConstraintMode.NO_CONSTRAINT)
    @JoinColumn(name = "annotation_type", 
        foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private AnnotationLayer attachType;

    @ManyToOne
    @JoinColumn(name = "annotation_feature",
        foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private AnnotationFeature attachFeature;

    @ManyToOne
    @JoinColumn(name = "project")
    private Project project;

    @Column(name = "crossSentence")
    private boolean crossSentence;
    
    @Column(name = "showTextInHover")
    private boolean showTextInHover = true;

    @Column(name = "linkedListBehavior")
    private boolean linkedListBehavior;

    @Column(name = "anchoring_mode")
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.AnchoringModeType")
    private AnchoringMode anchoringMode = AnchoringMode.TOKENS;

    @Column(name = "overlap_mode")
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.OverlapModeType")
    private OverlapMode overlapMode = OverlapMode.NO_OVERLAP;

    @Column(name = "validation_mode")
    @Type(type = "de.tudarmstadt.ukp.clarin.webanno.model.ValidationModeType")
    private ValidationMode validationMode = ValidationMode.ALWAYS;

    // This column is no longer used and should be removed with the next major version.
    // At that time, a corresponding Liquibase changeset needs to be introduced as well.
    @Deprecated
    @Column(name = "multipleTokens")
    private boolean multipleTokens;
    
    // This column is no longer used and should be removed with the next major version
    // At that time, a corresponding Liquibase changeset needs to be introduced as well.
    @Deprecated
    @Column(name = "lockToTokenOffset")
    private boolean lockToTokenOffset = true;
    
    // This column is no longer used and should be removed with the next major version
    // At that time, a corresponding Liquibase changeset needs to be introduced as well.
    @Deprecated
    @Column(name = "allowSTacking")
    private boolean allowStacking;
    
    @Lob
    @Column(length = 64000)
    private String traits;
    
    public AnnotationLayer()
    {
        // Required
    }
    
    public AnnotationLayer(String aName, String aUiName, String aType, Project aProject,
            boolean aBuiltIn, AnchoringMode aAnchoringMode, OverlapMode aOverlapMode)
    {
        setName(aName);
        setUiName(aUiName);
        setProject(aProject);
        setBuiltIn(aBuiltIn);
        setType(aType);
        setAnchoringMode(aAnchoringMode);
        setOverlapMode(aOverlapMode);
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
     * @param typeId the id.
     */
    public void setId(Long typeId)
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

    public AnchoringMode getAnchoringMode()
    {
        return anchoringMode;
    }
    
    public void setAnchoringMode(AnchoringMode aAnchoringMode)
    {
        anchoringMode = aAnchoringMode;
    }

    public void setAnchoringMode(boolean aLockToTokenOffset, boolean aMultipleTokens)
    {
        if (!aLockToTokenOffset && !aMultipleTokens) {
            anchoringMode = AnchoringMode.CHARACTERS;
        }
        else if (aLockToTokenOffset && !aMultipleTokens) {
            anchoringMode = AnchoringMode.SINGLE_TOKEN;
        }
        else if (aLockToTokenOffset && aMultipleTokens) {
            anchoringMode = AnchoringMode.TOKENS;
        }
        else if (!aLockToTokenOffset && aMultipleTokens) {
            anchoringMode = AnchoringMode.TOKENS;
        }
    }

    public ValidationMode getValidationMode()
    {
        return validationMode;
    }

    public void setValidationMode(ValidationMode aValidationMode)
    {
        validationMode = aValidationMode;
    }

    public boolean isAllowStacking()
    {
        return asList(OverlapMode.ANY_OVERLAP, OverlapMode.STACKING_ONLY).contains(overlapMode);
    }

    @Deprecated
    public void setAllowStacking(boolean allowStacking)
    {
        this.allowStacking = allowStacking;
    }
    
    public OverlapMode getOverlapMode()
    {
        return overlapMode;
    }

    public void setOverlapMode(OverlapMode aOverlapMode)
    {
        overlapMode = aOverlapMode;
    }

    public boolean isCrossSentence()
    {
        return crossSentence;
    }

    public void setCrossSentence(boolean crossSentence)
    {
        this.crossSentence = crossSentence;
    }

    public boolean isShowTextInHover()
    {
        return showTextInHover;
    }

    public void setShowTextInHover(boolean showTextInHover)
    {
        this.showTextInHover = showTextInHover;
    }

    public boolean isLinkedListBehavior()
    {
        return linkedListBehavior;
    }

    /**
     * Controls whether the chain behaves like a linked list or like a set. When operating as a
     * set, chains are automatically threaded and no arrows and labels are displayed on arcs.
     * When operating as a linked list, chains are not threaded and arrows and labels are displayed
     * on arcs.
     *
     * @param aLinkedListBehavior whether to behave like a set.
     */
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

    public String getOnClickJavascriptAction()
    {
        return onClickJavascriptAction;
    }

    public void setOnClickJavascriptAction(String onClickAction)
    {
        this.onClickJavascriptAction = onClickAction;
    }

    public String getTraits()
    {
        return traits;
    }

    public void setTraits(String aTraits)
    {
        traits = aTraits;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("AnnotationLayer [name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }
}
