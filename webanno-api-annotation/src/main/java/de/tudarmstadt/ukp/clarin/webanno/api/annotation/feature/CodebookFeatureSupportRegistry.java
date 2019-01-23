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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.CodebookFeature;

public interface CodebookFeatureSupportRegistry
{
    String SERVICE_NAME = "codebookFeatureSupportRegistry";

    default List<FeatureType> getPrimitiveFeatureTypes()
    {
        List<FeatureType> allTypes = new ArrayList<>();

        for (FeatureSupport<?> featureSupport : getFeatureSupports()) {
            List<FeatureType> types = featureSupport.getPrimitiveFeatureTypes();
            types.stream().filter(it -> !it.isInternal()).forEach(allTypes::add);
        }

        allTypes.sort(comparing(FeatureType::getUiName));

        return allTypes;
    }
    List<FeatureSupport> getFeatureSupports();
    <T> FeatureSupport<T> getFeatureSupport(CodebookFeature aFeature);
    FeatureType getCodebookFeatureType(CodebookFeature aFeature);
}
