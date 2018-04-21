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
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

/**
 * This interface contains methods which are related to TagSet, Tag and Type for the annotation
 * project.
 */
public interface AnnotationSchemaService
{
    String SERVICE_NAME = "annotationService";
    
    /**
     * creates a {@link Tag} for a given {@link TagSet}. Combination of {@code tag name} and
     * {@code tagset name} should be unique
     *
     * @param tag
     *            the tag.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createTag(Tag tag);

    /**
     * creates a {@link TagSet} object in the database
     *
     * @param tagset
     *            the tagset.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createTagSet(TagSet tagset);

    /**
     * creates a type which will be a span, chain, or arc(relation) type. Currently the annotation
     * types are highly highly tied with the tagsets, one tagset per type. POS, Names Entity, and
     * coreference links are span types while coreference chains and dependency parsings are
     * arc(relation) types.
     *
     * @param type
     *            the type.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createLayer(AnnotationLayer type);

    void createFeature(AnnotationFeature feature);

    
    /**
     * Get Tag by its ID
     * 
     * @param id
     *            the tag id.
     * @return the tag.
     */
    Optional<Tag> getTag(long id);

    /**
     * gets a {@link Tag} using its name and a {@link TagSet}
     * 
     * @param tagName
     *            the tag name.
     * @param tagSet
     *            the tagset.
     * @return the tag.
     */
    Tag getTag(String tagName, TagSet tagSet);

    /**
     * Check if a tag with this name in the given tagset exists
     * 
     * @param tagName
     *            the tag name.
     * @param tagSet
     *            the tagset.
     * @return if the tag exists.
     */
    boolean existsTag(String tagName, TagSet tagSet);

    /**
     * Check if a {@link TagSet} with this name exists
     * 
     * @param name
     *            the tagset name.
     * @param project
     *            the project.
     * @return if the tagset exists.
     */
    boolean existsTagSet(String name, Project project);

    /**
     * check if a {@link TagSet} in this {@link Project} exists
     * 
     * @param project
     *            the project.
     * @return if any tagset exists.
     */
    boolean existsTagSet(Project project);

    /**
     * Check if an {@link AnnotationLayer} exists with this name in the given {@link Project}.
     * 
     * @param name
     *            the layer name.
     * @param project
     *            the project.
     * @return if the layer exists.
     */
    boolean existsLayer(String name, Project project);

    /**
     * check if an {@link AnnotationLayer} exists with this name and type in this {@link Project}
     * 
     * @param name
     *            the layer name.
     * @param type
     *            the layer type.
     * @param project
     *            the project.
     * @return if the layer exists.
     */
    boolean existsLayer(String name, String type, Project project);

    /**
     * Check if this {@link AnnotationFeature} already exists
     * 
     * @param name
     *            the feature name.
     * @param type
     *            the feature type.
     * @return if the feature exists.
     */
    boolean existsFeature(String name, AnnotationLayer type);

    /**
     * get a {@link TagSet} with this name in a {@link Project}
     * 
     * @param name
     *            the tagset name.
     * @param project
     *            the project.
     * @return the tagset.
     */
    TagSet getTagSet(String name, Project project);

    /**
     * Get Tagset by its ID
     * 
     * @param id
     *            the tagset id.
     * @return the tagset.
     */
    TagSet getTagSet(long id);

    /**
     * Get an annotation layer using its id
     * 
     * @param id
     *            the layer id.
     * @return the layer.
     */
    AnnotationLayer getLayer(long id);

    /**
     * Get an {@link AnnotationLayer}
     * 
     * @param name
     *            the layer name.
     * @param project
     *            the project.
     * @return the layer.
     */
    AnnotationLayer getLayer(String name, Project project);

    AnnotationLayer getLayer(Project aProject, FeatureStructure aFS);
    
    /**
     * Get a {@link AnnotationFeature} name using its ID.
     *
     * @param id
     *            the feature id.
     * @return the feature.
     */
    AnnotationFeature getFeature(long id);

    /**
     * Get an {@link AnnotationFeature} using its name
     * 
     * @param name
     *            the feature name.
     * @param type
     *            the feature type.
     * @return the feature.
     */
    AnnotationFeature getFeature(String name, AnnotationLayer type);

    /**
     * Check if an {@link AnnotationLayer} already exists.
     * 
     * @param name
     *            the layer name.
     * @param type
     *            the layer type.
     * @return if the layer exists.
     */
    boolean existsType(String name, String type);

    /**
     * Initialize the project with default {@link AnnotationLayer}, {@link TagSet}s, and {@link Tag}
     * s. This is done per Project.
     * 
     * @param aProject
     *            the project.
     * @throws IOException
     *             if an I/O error occurs.
     */ 
    void initializeProject(Project aProject)
            throws IOException;
    
    /**
     * list all {@link AnnotationLayer} in the system
     *
     * @return the layers.
     */
    List<AnnotationLayer> listAnnotationType();

    /**
     * List all annotation types in a project
     * 
     * @param project
     *            the project.
     * @return the layers.
     */
    List<AnnotationLayer> listAnnotationLayer(Project project);
    
    /**
     * List all relation layers that are attached directly or indirectly (via a attach feature) to
     * the given layer. This method is useful to identify relation layers affected by a span delete
     * operation.
     * 
     * @param layer
     *            typically a span layer to be deleted.
     * @return the relation layers attaching directly or indirectly to the given layer.
     */
    List<AnnotationLayer> listAttachedRelationLayers(AnnotationLayer layer);
    
    /**
     * List all link features that could potentially link to the annotations of the given layer.
     * These include link features that have the given layer as a target type as well as link
     * features that have {@link CAS#TYPE_NAME_ANNOTATION} as the target type.
     * 
     * @param layer
     *            the target layer.
     * @return the possible link features.
     */
    List<AnnotationFeature> listAttachedLinkFeatures(AnnotationLayer layer);
     
    /**
     * List all the features in a {@link AnnotationLayer} for this {@link Project}
     * 
     * @param type
     *            the layer.
     * 
     * @return the features.
     */
    List<AnnotationFeature> listAnnotationFeature(AnnotationLayer type);

    /**
     * List all features in the project
     * 
     * @param project
     *            the project.
     * @return the features.
     */
    List<AnnotationFeature> listAnnotationFeature(Project project);

    /**
     * list all {@link Tag} in the system
     *
     * @return the tags.
     */
    List<Tag> listTags();

    /**
     * list all {@link Tag} in a {@link TagSet}
     *
     * @param tag
     *            the tagsset.
     * @return the tags.
     */
    List<Tag> listTags(TagSet tag);

    /**
     * list all {@link TagSet} in the system
     *
     * @return the tagsets.
     */
    List<TagSet> listTagSets();

    /**
     * List all {@link TagSet }s in a {@link Project}
     *
     * @param project
     *            the project.
     * @return the tagsets.
     */
    List<TagSet> listTagSets(Project project);

    /**
     * Removes a {@link Tag} from the database
     *
     * @param tag the tag.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeTag(Tag tag);

    /**
     * removes a {@link TagSet } from the database
     *
     * @param tagset the tagset.
     */
    void removeTagSet(TagSet tagset);
    
    /**
     * Removes all tags linked to a tagset
     */
    void removeAllTags(TagSet tagSet);

    /**
     * Should be called with care. Only when a project hosting this feature is removed
     * 
     * @param feature the feature.
     */
    void removeAnnotationFeature(AnnotationFeature feature);

    /**
     * Should be called with care. Only when a project hosting this layer is removed
     * 
     * @param type the type.
     */
    void removeAnnotationLayer(AnnotationLayer type);

    TagSet createTagSet(String aDescription, String aLanguage, String aTagSetName, String[] aTags,
            String[] aTagDescription, Project aProject)
                throws IOException;
    
    TypeSystemDescription getProjectTypes(Project aProject);
    
    void upgradeCas(CAS aCurCas, AnnotationDocument annotationDocument)
            throws UIMAException, IOException;
    
    void upgradeCas(CAS aCas, SourceDocument aSourceDocument, String aUser)
            throws UIMAException, IOException;

    TypeAdapter getAdapter(AnnotationLayer aLayer);
}
