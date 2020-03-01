/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer;

import static java.util.Comparator.comparing;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public interface LayerSupportRegistry
{
    List<LayerSupport> getLayerSupports();

    LayerSupport getLayerSupport(AnnotationLayer aLayer);

    LayerSupport getLayerSupport(String aId);

    LayerType getLayerType(AnnotationLayer aLayer);

    /**
     * Get the types of all layers the user should be able to create. There can also be internal
     * types reserved for built-in features. These are not returned.
     */
    default List<LayerType> getAllTypes()
    {
        List<LayerType> allTypes = new ArrayList<>();

        for (LayerSupport<?,?> layerSupport : getLayerSupports()) {
            List<LayerType> types = layerSupport.getSupportedLayerTypes();
            types.stream()
                    .filter(l -> !l.isInternal())
                    .forEach(allTypes::add);
        }

        allTypes.sort(comparing(LayerType::getUiName));

        return allTypes;
    }
}
