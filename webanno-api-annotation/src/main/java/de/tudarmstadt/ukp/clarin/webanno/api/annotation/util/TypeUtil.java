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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

/**
 * Utility Class for {@link TypeAdapter} with static methods such as geting
 * {@link TypeAdapter} based on its {@link CAS} {@link Type}
 *
 */
public final class TypeUtil
{
	private TypeUtil() {
		// No instances
	}
	
	public static AnnotationLayer getLayer(AnnotationSchemaService aRepo, Project aProject, 
	        FeatureStructure aFS)
	{
        String layerName = aFS.getType().getName();
        AnnotationLayer layer;
        try {
            layer = aRepo.getLayer(layerName, aProject);
        }
        catch (NoResultException e) {
            if (layerName.endsWith("Chain")) {
                layerName = layerName.substring(0, layerName.length() - 5);
            }
            if (layerName.endsWith("Link")) {
                layerName = layerName.substring(0, layerName.length() - 4);
            }
            layer = aRepo.getLayer(layerName, aProject);
        }
        
        return layer;
	}

    public static TypeAdapter getAdapter(AnnotationSchemaService aRepo, AnnotationLayer aLayer)
    {
        switch (aLayer.getType()) {
            case WebAnnoConst.SPAN_TYPE: {
                SpanAdapter adapter = new SpanAdapter(aLayer, aRepo.listAnnotationFeature(aLayer));
                adapter.setLockToTokenOffsets(aLayer.isLockToTokenOffset());
                adapter.setAllowStacking(aLayer.isAllowStacking());
                adapter.setAllowMultipleToken(aLayer.isMultipleTokens());
                adapter.setCrossMultipleSentence(aLayer.isCrossSentence());
                return adapter;
            }
            case WebAnnoConst.RELATION_TYPE: {
                ArcAdapter adapter = new ArcAdapter(aLayer, aLayer.getId(), aLayer.getName(),
                    WebAnnoConst.FEAT_REL_TARGET, WebAnnoConst.FEAT_REL_SOURCE,
                    aLayer.getAttachFeature() == null ? null : aLayer.getAttachFeature().getName(),
                    aLayer.getAttachType().getName(), aRepo.listAnnotationFeature(aLayer));

                adapter.setCrossMultipleSentence(aLayer.isCrossSentence());
                adapter.setAllowStacking(aLayer.isAllowStacking());

                return adapter;
                // default is chain (based on operation, change to CoreferenceLinK)
            }
            case WebAnnoConst.CHAIN_TYPE: {
                ChainAdapter adapter = new ChainAdapter(aLayer, aLayer.getId(), aLayer.getName()
                    + ChainAdapter.CHAIN, aLayer.getName(), "first", "next",
                    aRepo.listAnnotationFeature(aLayer));

                adapter.setLinkedListBehavior(aLayer.isLinkedListBehavior());

                return adapter;
            }
            default:
                throw new IllegalArgumentException("No adapter for type with name [" + aLayer.getName()
                    + "]");
        }
    }
    
    /**
     * Construct the label text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aFeatures the features.
     * @return the label.
     */
    public static String getUiLabelText(TypeAdapter aAdapter, Map<String, String> aFeatures)
    {
        StringBuilder bratLabelText = new StringBuilder();
        for (Entry<String, String> feature : aFeatures.entrySet()) {
            String label = StringUtils.defaultString(feature.getValue());
            
            if (bratLabelText.length() > 0 && label.length() > 0) {
                bratLabelText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            bratLabelText.append(label);
        }

        if (bratLabelText.length() > 0) {
            return bratLabelText.toString();
        }
        else {
            // If there are no label features at all, then use the layer UI name
            return "(" + aAdapter.getLayer().getUiName() + ")";
        }
    }
    
    /**
     * Construct the label text used in the brat user interface.
     *
     * @param aAdapter the adapter.
     * @param aFs the annotation.
     * @param aFeatures the features.
     * @return the label.
     */
    public static String getUiLabelText(TypeAdapter aAdapter, AnnotationFS aFs,
            List<AnnotationFeature> aFeatures)
    {
        StringBuilder bratLabelText = new StringBuilder();
        for (AnnotationFeature feature : aFeatures) {

            if (!feature.isEnabled() || !feature.isVisible()
                    || !MultiValueMode.NONE.equals(feature.getMultiValueMode())) {
                continue;
            }

            Feature labelFeature = aFs.getType().getFeatureByBaseName(feature.getName());
            String label = StringUtils.defaultString(aFs.getFeatureValueAsString(labelFeature));
            
            if (bratLabelText.length() > 0 && label.length() > 0) {
                bratLabelText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            bratLabelText.append(label);
        }

        if (bratLabelText.length() > 0) {
            return bratLabelText.toString();
        }
        else {
            // If there are no label features at all, then use the layer UI name
            return "(" + aAdapter.getLayer().getUiName() + ")";
        }
    }

    /**
     * @param aBratTypeName the brat type name.
     * @return the layer ID.
     * @see #getUiTypeName
     */
    public static long getLayerId(String aBratTypeName)
    {
        return Long.parseLong(aBratTypeName.substring(0, aBratTypeName.indexOf("_")));
    }

    public static String getUiTypeName(TypeAdapter aAdapter)
    {
        return aAdapter.getTypeId() + "_" + aAdapter.getAnnotationTypeName();
    }

    public static String getUiTypeName(AnnotationLayer aLayer)
    {
        return aLayer.getId() + "_" + aLayer.getName();
    }
}
