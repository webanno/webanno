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

import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.springframework.security.access.prepost.PreAuthorize;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * This interface contains methods which are related to TagSet, Tag and Type for the annotation
 * project.
 */
public interface CodebookSchemaService
{
    String SERVICE_NAME = "codebookService";
    
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    void createCodebook(Codebook codebook);
    
    void createCodebookFeature(CodebookFeature aFeature);

    boolean existsFeature(String aName, Codebook aCodebook);
    
    boolean existsCodebook (String name, Project project);
    
    Codebook getCodebook(long id);
    
    Codebook getCodebook(int codebookorder, Project project);
    
    /**
     * Get a {@code Codebook}
     * @param aName Name of the codebook
     * @param aProject the current project
     * @return
     */
    Codebook getCodeBook(String aName, Project aProject);
    
    CodebookFeature getCodebookFeature(String name, Codebook codebook);
    
    /**
     * List all codebooks in the project
     * @param aProject
     * @return
     */
    List<Codebook> listCodebook(Project aProject);
    
    /**
     * List all features in this {@code Codebook}
     * @param code the codebook code
     * @return the list of features in this codebook
     */
    List<CodebookFeature> listCodebookFeature(Codebook code);

    List<CodebookFeature> listCodebookFeature(Project aProject);
    
    void removeCodebookFeature(CodebookFeature feature);
    
    void removeCodebook(Codebook type);
    
    void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD,
            Codebook aCodebook);
    
    
}
