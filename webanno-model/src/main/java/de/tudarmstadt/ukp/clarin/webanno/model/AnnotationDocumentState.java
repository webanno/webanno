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

import de.tudarmstadt.ukp.clarin.webanno.support.PersistentEnum;

/**
 * Variables for the different states of a {@link AnnotationDocument} workflow.
 *
 *
 */
public enum AnnotationDocumentState implements PersistentEnum
{
    /**
     * For every source document, there will be a NEW annotation document, untill the user start
     * annotating it.
     */
    NEW("NEW", "black"),
    
    /**
     *
     * annotation document has been created for this document for this annotator
     */
    IN_PROGRESS("INPROGRESS", "blue"),
    
    /**
     * annotator has marked annotation document as complete
     */
    FINISHED("FINISHED", "red"),
    
    /**
     * Ignore this annotation document from further processing such as curation
     */
    IGNORE("IGNORE", "black");

    private final String id;
    private final String color;

    AnnotationDocumentState(String aId, String aColor)
    {
        id = aId;
        color = aColor;
    }

    public String getName()
    {
        return getId();
    }
    
    @Override
    public String getId()
    {
        return id;
    }
    
    public String getColor()
    {
        return color;
    }
    
    @Override
    public String toString()
    {
        return getId();
    }
}
