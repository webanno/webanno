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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;

public class CodebookFeatureState
    implements Serializable
{
    private static final long serialVersionUID = 3512979848975446735L;
    public final CodebookFeature feature;
    public Serializable value;
    public List<Tag> tagset;

    public CodebookFeatureState(CodebookFeature aFeature, Serializable aValue)
    {
        feature = aFeature;
        value = aValue;

        if (value == null ) {
            value = new ArrayList<>();
        }
    }
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
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
        CodebookFeatureState other = (CodebookFeatureState) obj;
        if (feature == null) {
            if (other.feature != null) {
                return false;
            }
        }
        else if (!feature.equals(other.feature)) {
            return false;
        }
        return true;
    }
}
