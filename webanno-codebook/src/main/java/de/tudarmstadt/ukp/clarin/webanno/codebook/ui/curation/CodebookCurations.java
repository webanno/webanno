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

import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;

public class CodebookCurations
    implements Serializable
{
    private static final long serialVersionUID = -338943332519737951L;
    private String annotation;
    private boolean hasDiff;
    private Codebook codebook;
    private CodebookFeature feature;

    public CodebookCurations()
    {
        //
    }

    public CodebookCurations(String aAnnotation, boolean aHasDiff, Codebook aCodebook,
            CodebookFeature aFeature)
    {
        this.annotation = aAnnotation;
        this.hasDiff = aHasDiff;
        this.codebook = aCodebook;
        this.feature = aFeature;
    }

    public String getAnnotation()
    {
        return annotation;
    }

    public void setAnnotation(String annotation)
    {
        this.annotation = annotation;
    }

    public boolean isHasDiff()
    {
        return hasDiff;
    }

    public void setHasDiff(boolean hasDiff)
    {
        this.hasDiff = hasDiff;
    }

    public Codebook getCodebook()
    {
        return codebook;
    }

    public void setCodebook(Codebook codebook)
    {
        this.codebook = codebook;
    }

    public CodebookFeature getFeature()
    {
        return feature;
    }

    public void setFeature(CodebookFeature feature)
    {
        this.feature = feature;
    }

}
