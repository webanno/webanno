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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter;

import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A type adapter encapsulates a specific kind of annotation layer, e.g. spans, relations or chains.
 * A type adapter can be obtained via {@link AnnotationSchemaService#getAdapter} and is always
 * created for one concrete layer (e.g. POS, NamedEntity). It allows interacting with annotations on
 * the layer, e.g. creating annotations, deleting annotations, setting and getting feature values.
 * Most actions are defined by implementations of this interface such as {@link SpanAdapter},
 * {@link ArcAdapter} or {@link ChainAdapter}.
 */
public interface TypeAdapter
{
    /**
     * String used to separate multiple values in the annotation label displayed in the UI.
     */
    String FEATURE_SEPARATOR = " | ";

    /**
     * @return the database ID of the type.
     */
    long getTypeId();

    /**
     * Get the CAS type of the this {@link TypeAdapter}
     *
     * @param cas
     *            the CAS.
     * @return the type.
     */
    Type getAnnotationType(CAS cas);

    /**
     * Get the CAS type of the this {@link TypeAdapter}
     *
     * @return the type.
     */
    String getAnnotationTypeName();

    /**
     * If the underlying layer is attached to another layer, get the name of the feature in the
     * other layer which is used to refer to annotations on the underlying layer. For example, every
     * annotation on the {@link POS} layer must correspond to an annotation on the {@link Token}
     * layer and the token feature {@link Token#getPos()} must point the POS annotation. Thus, for
     * the POS layer, this method would return {@code pos} and the method
     * {@link #getAttachTypeName()} would return
     * {@code de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token}.
     *
     * @return the attach feature name.
     */
    String getAttachFeatureName();

    /**
     * @return the name of the layer the current layer attaches to (if any)
     * 
     * @see #getAttachFeatureName()
     */
    String getAttachTypeName();

    /**
     * Check if the annotation type is deletable
     *
     * @return if the layer is deletable.
     */
    boolean isDeletable();

    /**
     * Delete a annotation from CAS.
     *
     * @param aJCas
     *            the CAS object
     * @param aVid
     *            the VID of the object to be deleted.
     */
    void delete(AnnotatorState aState, JCas aJCas, VID aVid);

    /**
     * @return the layer for which this adapter has been created.
     */
    AnnotationLayer getLayer();

    /**
     * @return the features defined for this layer.
     * 
     * @see AnnotationSchemaService#listAnnotationFeature(AnnotationLayer)
     */
    Collection<AnnotationFeature> listFeatures();

    /**
     * Set the value of the given feature.
     * 
     * @param aJcas
     *            the JCas.
     * @param aAddress
     *            the annotation ID.
     * @param aFeature
     *            the feature.
     * @param aValue
     *            the value.
     */
    void setFeatureValue(AnnotatorState aState, JCas aJcas, int aAddress,
            AnnotationFeature aFeature, Object aValue);

    /**
     * Get the value of the given feature.
     * 
     * @param aFeature
     *            the feature.
     * @param aFs
     *            the annotation.
     * @return the feature value.
     */
    <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs);
}
