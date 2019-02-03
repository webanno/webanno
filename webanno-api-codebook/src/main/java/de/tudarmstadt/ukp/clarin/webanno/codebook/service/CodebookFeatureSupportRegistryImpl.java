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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;

@Component
public class CodebookFeatureSupportRegistryImpl
    implements CodebookFeatureSupportRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<CodebookFeatureSupport> featureSupportsProxy;
    
    private List<CodebookFeatureSupport> featureSupports;
    
    private final Map<Long, CodebookFeatureSupport> supportCache = new HashMap<>();

    public CodebookFeatureSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<CodebookFeatureSupport> aFeatureSupports)
    {
        featureSupportsProxy = aFeatureSupports;
    }
    
    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }
    
    public void init()
    {
        List<CodebookFeatureSupport> fsp = new ArrayList<>();

        if (featureSupportsProxy != null) {
            fsp.addAll(featureSupportsProxy);
            AnnotationAwareOrderComparator.sort(fsp);
        
            for (CodebookFeatureSupport<?> fs : fsp) {
                log.info("Found feature support: {}",
                        ClassUtils.getAbbreviatedName(fs.getClass(), 20));
            }
        }
        
        featureSupports = Collections.unmodifiableList(fsp);
    }
    
    @Override
    public FeatureType getCodebookFeatureType(CodebookFeature aFeature)
    {
        if (aFeature.getType() == null) {
            return null;
        }
    
        FeatureType featureType = null;
        for (CodebookFeatureSupport<?> s : getFeatureSupports()) {
            Optional<FeatureType> ft = s.getCodebookFeatureType(aFeature);
            if (ft.isPresent()) {
                featureType = ft.get();
                break;
            }
        }
        return featureType;
    }
    
    @Override
    public CodebookFeatureSupport getFeatureSupport(CodebookFeature aFeature) {
        CodebookFeatureSupport support = null;

        if (aFeature.getId() != null) {
            support = supportCache.get(aFeature.getId());
        }

        if (support == null) {
            for (CodebookFeatureSupport<?> s : getFeatureSupports()) {
                support = s;
                if (aFeature.getId() != null) {
                    // Store feature in the cache, but only when it has an ID, i.e. it has
                    // actually been saved.
                    supportCache.put(aFeature.getId(), s);
                }
                break;
            }
        }

        if (support == null) {
            throw new IllegalArgumentException("Unsupported feature: [" + aFeature.getName() + "]");
        }

        return support;
    }
    
    @Override
    public List<CodebookFeatureSupport> getFeatureSupports()
    {
        return featureSupports;
    }

    @Override
    public CodebookFeatureSupport getFeatureSupport(String aId)
    {
        return getFeatureSupports().stream().filter(fs -> fs.getId().equals(aId)).findFirst()
                .orElse(null);
    }
}
