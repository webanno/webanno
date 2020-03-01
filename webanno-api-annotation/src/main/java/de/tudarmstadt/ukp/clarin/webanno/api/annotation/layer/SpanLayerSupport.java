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

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanLayerBehavior;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.SpanRenderer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

@Component
public class SpanLayerSupport
    extends LayerSupport_ImplBase<SpanAdapter, SpanLayerTraits>
    implements InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ApplicationEventPublisher eventPublisher;
    private final AnnotationSchemaService schemaService;
    private final LayerBehaviorRegistry layerBehaviorsRegistry;
    
    private String layerSupportId;
    private List<LayerType> types;

    public SpanLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationSchemaService aSchemaService,
            LayerBehaviorRegistry aLayerBehaviorsRegistry)
    {
        super(aFeatureSupportRegistry);
        eventPublisher = aEventPublisher;
        schemaService = aSchemaService;
        layerBehaviorsRegistry = aLayerBehaviorsRegistry;
    }

    @Override
    public String getId()
    {
        return layerSupportId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        layerSupportId = aBeanName;
    }
    
    @Override
    public void afterPropertiesSet() throws Exception
    {
        types = asList(new LayerType(WebAnnoConst.SPAN_TYPE, "Span", layerSupportId));
    }
    
    @Override
    public List<LayerType> getSupportedLayerTypes()
    {
        return types;
    }
    
    @Override
    public boolean accepts(AnnotationLayer aLayer)
    {
        return WebAnnoConst.SPAN_TYPE.equals(aLayer.getType());
    }
    
    @Override
    public SpanAdapter createAdapter(AnnotationLayer aLayer)
    {
        SpanAdapter adapter = new SpanAdapter(getLayerSupportRegistry(), featureSupportRegistry,
                eventPublisher, aLayer, () -> schemaService.listAnnotationFeature(aLayer),
                layerBehaviorsRegistry.getLayerBehaviors(this, SpanLayerBehavior.class));
        
        return adapter;
    }
    
    @Override
    public void generateTypes(TypeSystemDescription aTsd, AnnotationLayer aLayer,
            List<AnnotationFeature> aAllFeaturesInProject)
    {
        TypeDescription td = aTsd.addType(aLayer.getName(), aLayer.getDescription(),
                CAS.TYPE_NAME_ANNOTATION);
        
        List<AnnotationFeature> featureForLayer = aAllFeaturesInProject.stream()
                .filter(feature -> aLayer.equals(feature.getLayer()))
                .collect(toList());
        generateFeatures(aTsd, td, featureForLayer);
    }
    
    @Override
    public SpanRenderer getRenderer(AnnotationLayer aLayer)
    {
        return new SpanRenderer(createAdapter(aLayer), getLayerSupportRegistry(),
                featureSupportRegistry,
                layerBehaviorsRegistry.getLayerBehaviors(this, SpanLayerBehavior.class));
    }
    
    @Override
    public Panel createTraitsEditor(String aId,  IModel<AnnotationLayer> aLayerModel)
    {
        AnnotationLayer layer = aLayerModel.getObject();
        
        if (!accepts(layer)) {
            throw unsupportedLayerTypeException(layer);
        }
        
        return new SpanLayerTraitsEditor(aId, this, aLayerModel);
    }

    @Override
    public SpanLayerTraits readTraits(AnnotationLayer aFeature)
    {
        SpanLayerTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(SpanLayerTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            log.error("Unable to read traits", e);
        }
    
        if (traits == null) {
            traits = new SpanLayerTraits();
        }
    
        return traits;
    }
    
    @Override
    public void writeTraits(AnnotationLayer aFeature, SpanLayerTraits aTraits)
    {
        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            log.error("Unable to write traits", e);
        }
    }
}
