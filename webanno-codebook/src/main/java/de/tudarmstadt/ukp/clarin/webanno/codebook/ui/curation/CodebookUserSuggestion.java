/*
 * Copyright 2020
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.curation;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CodebookUserSuggestion
    implements Serializable
{
    private static final long serialVersionUID = 2749434980944442163L;

    private String user;
    private Codebook codebook;
    private String value;
    private SourceDocument document;
    private boolean hasDiff;

    public CodebookUserSuggestion(String user, Codebook codebook, String value,
            SourceDocument document, boolean hasDiff)
    {
        this.user = user;
        this.codebook = codebook;
        this.value = value;
        this.document = document;
        this.hasDiff = hasDiff;
    }

    public String getUser()
    {
        return user;
    }

    public Codebook getCodebook()
    {
        return codebook;
    }

    public String getValue()
    {
        return value;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public boolean hasDiff()
    {
        return hasDiff;
    }

    public void setDiff(boolean hasDiff)
    {
        this.hasDiff = hasDiff;
    }
}
