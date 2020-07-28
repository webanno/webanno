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
package de.tudarmstadt.ukp.clarin.webanno.codebook.service;

import java.util.List;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * This interface contains methods which are related to Codebook, CodebookTag and Type for the
 * annotation project.
 */
public interface CodebookSchemaService
{
    String SERVICE_NAME = "codebookService";

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createCodebook(Codebook codebook);

    void createCodebookFeature(CodebookFeature aFeature);

    /**
     * creates a {@link CodebookTag} for a given {@link Codebook}. Combination of {@code tag name}
     * and {@code codebook name} should be unique
     *
     * @param aTag
     *            the tag.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createCodebookTag(CodebookTag aTag);

    boolean existsFeature(String aName, Codebook aCodebook);

    boolean existsCodebook(String name, Project project);

    /**
     * Check if a tag with this name in the given codebook exists
     *
     * @param tagName
     *            the tag name.
     * @param aCodebook
     *            the codebook.
     * @return if the tag exists.
     */
    boolean existsCodebookTag(String tagName, Codebook aCodebook);

    Codebook getCodebook(long id);

    Codebook getCodebook(int codebookorder, Project project);

    /**
     * Get a {@code Codebook}
     * 
     * @param aName
     *            Name of the codebook
     * @param aProject
     *            the current project
     * @return
     */
    Codebook getCodeBook(String aName, Project aProject);

    CodebookFeature getCodebookFeature(String name, Codebook codebook);

    /**
     * gets a {@link CodebookTag} using its name and a {@link Codebook}
     *
     * @param tagName
     *            the tag name.
     * @param aCodebook
     *            the codebook.
     * @return the tag.
     */
    CodebookTag getCodebookTag(String tagName, Codebook aCodebook);

    /**
     * Removes a {@link CodebookTag} from the database
     *
     * @param tag
     *            the tag.
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void removeCodebookTag(CodebookTag tag);

    /**
     * list all {@link CodebookTag} in a {@link Codebook}
     *
     * @param aCodebook
     *            the codebook.
     * @return the tags.
     */
    List<CodebookTag> listTags(Codebook aCodebook);

    /**
     * List all codebooks in the project
     * 
     * @param aProject
     * @return
     */
    List<Codebook> listCodebook(Project aProject);

    /**
     * List all features in this {@code Codebook}
     * 
     * @param code
     *            the codebook code
     * @return the list of features in this codebook
     */
    List<CodebookFeature> listCodebookFeature(Codebook code);

    List<CodebookFeature> listCodebookFeature(Project aProject);

    void removeCodebookFeature(CodebookFeature feature);

    void removeCodebook(Codebook type);

    void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD, Codebook aCodebook);

    /**
     * Returns a type system with all the codebook types that should be present in an exported CAS.
     * This means in particular that type internal to the application should <b>not</b> be included.
     */
    TypeSystemDescription getCodebookTypeSystemForExport(Project aProject)
        throws ResourceInitializationException;

}
