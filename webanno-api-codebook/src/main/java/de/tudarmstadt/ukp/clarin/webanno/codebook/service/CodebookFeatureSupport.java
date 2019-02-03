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
package de.tudarmstadt.ukp.clarin.webanno.codebook.service;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;

import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.BeanNameAware;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;

/**
 * Extension point for new types of annotation features. 
 * 
 * @param <T> the traits type. If no traits are supported, this should be {@link Void}.
 */
public interface CodebookFeatureSupport<T>
    extends BeanNameAware
{
    String getId();
    
    void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD, 
            CodebookFeature aFeature);

    default void setFeatureValue(JCas aJcas, CodebookFeature aFeature, int aAddress,
            Object aValue)
    {
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, aValue);
    }
    
    <V> V getFeatureValue(CodebookFeature aFeature, FeatureStructure aFS);
    
    default IllegalArgumentException unsupportedFeatureTypeException(CodebookFeature aFeature)
    {
        return new IllegalArgumentException("Unsupported type [" + aFeature.getType()
                + "] on feature [" + aFeature.getName() + "]");
    }

    
    default Optional<FeatureType> getCodebookFeatureType(CodebookFeature aCodebookFeature)
    {
        return getPrimitiveFeatureTypes().stream()
                .filter(t -> t.getName().equals(aCodebookFeature.getType())).findFirst();
    }

    default Panel createCodebookTraitsEditor(String aId, IModel<CodebookFeature> aFeatureModel)
    {
        return new EmptyPanel(aId);
    }
    
    List<FeatureType> getPrimitiveFeatureTypes();
    
    public static void setFeature(FeatureStructure aFS, CodebookFeature aFeature, Object aValue)
    {
        if (aFeature == null) {
            return;
        }
        Feature feature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        aFS.setStringValue(feature, (String) aValue);
    }
    default void configureCodeBookFeature(CodebookFeature aFeature)
    {
        // Nothing to do
    }
}
