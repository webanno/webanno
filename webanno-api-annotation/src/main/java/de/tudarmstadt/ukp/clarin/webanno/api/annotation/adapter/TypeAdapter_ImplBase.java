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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class TypeAdapter_ImplBase
    implements TypeAdapter
{
    private final LayerSupportRegistry layerSupportRegistry;
    
    private final FeatureSupportRegistry featureSupportRegistry;
    
    private final AnnotationLayer layer;
    
    private final Supplier<Collection<AnnotationFeature>> featureSupplier;

    private Map<String, AnnotationFeature> features;

    private ApplicationEventPublisher applicationEventPublisher;

    private Map<AnnotationLayer, Object> layerTraitsCache;

    /**
     * Constructor.
     * 
     * @param aLayerSupportRegistry
     *            the layer support registry to allow e.g. convenient decoding of layer traits.
     * @param aFeatureSupportRegistry
     *            the feature support registry to allow e.g. convenient generation of features or
     *            getting/setting feature values.
     * @param aEventPublisher
     *            an optional publisher for Spring events.
     * @param aLayer
     *            the layer for which the adapter is created.
     * @param aFeatures
     *            supplier for the features, typically
     *            {@link AnnotationSchemaService#listAnnotationFeature(AnnotationLayer)}. Since the
     *            features are not always needed, we use a supplied here so they can be loaded
     *            lazily. To facilitate testing, we do not pass the entire
     *            {@link AnnotationSchemaService}.
     */
    public TypeAdapter_ImplBase(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        layerSupportRegistry = aLayerSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
        applicationEventPublisher = aEventPublisher;
        layer = aLayer;
        featureSupplier = aFeatures;
    }
    
    @Override
    public AnnotationLayer getLayer()
    {
        return layer;
    }
    
    @Override
    public Collection<AnnotationFeature> listFeatures()
    {
        if (features == null) {
            // Using a sorted map here so we have reliable positions in the map when iterating. We
            // use these positions to remember the armed slots!
            features = new TreeMap<>();
            for (AnnotationFeature f : featureSupplier.get()) {
                features.put(f.getName(), f);
            }
        }
        
        return features.values();
    }
    
    @Override
    public void setFeatureValue(SourceDocument aDocument, String aUsername, CAS aCas,
            int aAddress, AnnotationFeature aFeature, Object aValue)
    {
        FeatureStructure fs = selectFsByAddr(aCas, aAddress);

        Object oldValue = getValue(fs, aFeature);
        
        featureSupportRegistry.getFeatureSupport(aFeature).setFeatureValue(aCas, aFeature,
                aAddress, aValue);

        Object newValue = getValue(fs, aFeature);
        
        if (!Objects.equals(oldValue, newValue)) {
            publishEvent(new FeatureValueUpdatedEvent(this, aDocument, aUsername, getLayer(), fs,
                    aFeature, newValue, oldValue));
        }
    }
    
    private Object getValue(FeatureStructure fs, AnnotationFeature aFeature)
    {
        Object value;
        
        Feature f = fs.getType().getFeatureByBaseName(aFeature.getName());
        if (f.getRange().isPrimitive()) {
            value = FSUtil.getFeature(fs, aFeature.getName(), Object.class);
        }
        else if (FSUtil.isMultiValuedFeature(fs, f)) {
            value = FSUtil.getFeature(fs, aFeature.getName(), List.class);
        }
        else {
            value = FSUtil.getFeature(fs, aFeature.getName(), FeatureStructure.class);
        }
        
        return value;
    }

    @Override
    public <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        return (T) featureSupportRegistry.getFeatureSupport(aFeature).getFeatureValue(aFeature,
                aFs);
    }
    
    public void publishEvent(ApplicationEvent aEvent)
    {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(aEvent);
        }
    }
    
    @Override
    public void initialize(AnnotationSchemaService aSchemaService)
    {
        // Nothing to do
    }
    
    @Override
    public String getAttachFeatureName()
    {
        return getLayer().getAttachFeature() == null ? null
                : getLayer().getAttachFeature().getName();
    }

    /**
     * A field that takes the name of the annotation to attach to, e.g.
     * "de.tudarmstadt...type.Token" (Token.class.getName())
     */
    @Override
    public String getAttachTypeName()
    {
        return getLayer().getAttachType() == null ? null : getLayer().getAttachType().getName();
    }
    
    @Override
    public void silenceEvents()
    {
        applicationEventPublisher = null;
    }

    /**
     * Decodes the traits for the current layer and returns them if they implement the requested
     * interface. This method internally caches the decoded traits, so it can be called often.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTraits(Class<T> aInterface)
    {
        if (layerTraitsCache == null) {
            layerTraitsCache = new HashMap<>();
        }
        
        Object trait = layerTraitsCache.computeIfAbsent(getLayer(), feature ->
               layerSupportRegistry.getLayerSupport(feature).readTraits(feature));
        
        if (trait != null && aInterface.isAssignableFrom(trait.getClass())) {
            return Optional.of((T) trait);
        }
        
        return Optional.empty();
    }
}
