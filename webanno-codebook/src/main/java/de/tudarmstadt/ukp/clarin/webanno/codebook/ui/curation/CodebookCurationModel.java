/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt 
 * and  Language Technology Universität Hamburg
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.CodebookFeatureState;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.suggestion.CodebookSuggestions;
import de.tudarmstadt.ukp.clarin.webanno.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CodebookCurationModel implements Serializable {
    private static final long serialVersionUID = -7826210722492638188L;
    private List<Codebook> codebooks;
    private SourceDocument document;
    private Project project;
    private List<CodebookCurations> codebookCurations = new ArrayList<>();
    private List<CodebookSuggestions> codebooksuggestions = new ArrayList<>();
    private final List<CodebookFeatureState> codebookModels = new ArrayList<>();

    public CodebookFeatureState getCodebookFeatureState(CodebookFeature aFeature) {
        for (CodebookFeatureState f : codebookModels) {
            if (Objects.equals(f.feature.getId(), aFeature.getId())) {
                return f;
            }
        }
        return null;
    }

    public CodebookCurationModel() {

    }

    public CodebookCurationModel(List<Codebook> aCodebooks, SourceDocument aDocument,
            Project aProject) {
        this.codebooks = aCodebooks;
        ;
        this.document = aDocument;
        this.project = aProject;
    }

    public List<Codebook> getCodebooks() {
        return codebooks;
    }

    public void setCodebooks(List<Codebook> codebooks) {
        this.codebooks = codebooks;
    }

    public SourceDocument getDocument() {
        return document;
    }

    public void setDocument(SourceDocument document) {
        this.document = document;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<CodebookFeatureState> getCodebookFeatureStates() {
        return codebookModels;
    }

    public List<CodebookCurations> getCodebookCurations() {
        return codebookCurations;
    }

    public void setCodebookCurations(List<CodebookCurations> codebookCurations) {
        this.codebookCurations = codebookCurations;
    }

    public List<CodebookSuggestions> getCodebooksuggestions() {
        return codebooksuggestions;
    }

    public void setCodebooksuggestions(List<CodebookSuggestions> codebooksuggestions) {
        this.codebooksuggestions = codebooksuggestions;
    }


}
