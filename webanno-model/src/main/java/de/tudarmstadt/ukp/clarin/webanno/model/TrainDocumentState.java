/*
 * Copyright 2017
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
 * Variables for the different states of a {@link TrainingDocument} workflow.
 */
public enum TrainDocumentState
    implements PersistentEnum
{
    /**
     * This is a newly added training document.
     */
    NEW("NEW"),
    
    /**
     * This training document is processed and used for training a model
     */
    CURATION_IN_PROGRESS("PROCESSED");

    public String getName()
    {
        return getId();
    }

    @Override
    public String toString()
    {
        return getId();
    }

    TrainDocumentState(String aId)
    {
        this.id = aId;
    }

    private final String id;

    @Override
    public String getId()
    {
        return id;
    }
}
