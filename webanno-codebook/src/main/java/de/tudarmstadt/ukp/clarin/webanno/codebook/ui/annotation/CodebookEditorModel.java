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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.annotation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookFeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;

public class CodebookEditorModel
    implements Serializable
{

    private static final long serialVersionUID = -628789175872734603L;
    private Codebook codebook;
    private String code;
    private int codebooksPerPage;
    private User user;
    private SourceDocument document;
    private Project project;

    private List<CodebookFeatureState> codebookFeatureStates = new ArrayList<>();

    public CodebookEditorModel()
    {

    }

    public CodebookEditorModel(Codebook aCodebook)
    {
        this.codebook = aCodebook;
    }

    public Codebook getCodebook()
    {
        return codebook;
    }

    public void setCodebook(Codebook codebook)
    {
        this.codebook = codebook;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public int getCodebooksPerPage()
    {
        return codebooksPerPage;
    }

    public void setCodebooksPerPage(int codebooksPerPage)
    {
        this.codebooksPerPage = codebooksPerPage;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public void setDocument(SourceDocument document)
    {
        this.document = document;
    }

    public List<CodebookFeatureState> getCodebookFeatureStates()
    {
        return codebookFeatureStates;
    }

    public void setCodebookFeatureStates(List<CodebookFeatureState> codebookFeatureStates)
    {
        this.codebookFeatureStates = codebookFeatureStates;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
    }

    public Project getProject()
    {
        return project;
    }

    public void setProject(Project project)
    {
        this.project = project;
    }

}
