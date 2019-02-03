/*
 * Copyright 2017
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

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.config.PrimitiveUimaFeatureSupportProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.UimaStringCodebookTraitsEditor;

@Component
public class PrimitiveUimaCodebookFeatureSupport
    implements CodebookFeatureSupport<Void>, InitializingBean
{
    private final PrimitiveUimaFeatureSupportProperties properties;
    
    private List<FeatureType> primitiveTypes;

    private String featureSupportId;
    
    /*
     * Constructor for use in unit tests to avoid having to always instantiate the properties.
     */
    public PrimitiveUimaCodebookFeatureSupport()
    {
        properties = new PrimitiveUimaFeatureSupportProperties();
    }

    @Autowired(required = true)
    public PrimitiveUimaCodebookFeatureSupport(PrimitiveUimaFeatureSupportProperties aProperties)
    {
        properties = aProperties;
    }

    @Override
    public String getId()
    {
        return featureSupportId;
    }
    

    @Override
    public Panel createCodebookTraitsEditor(String aId,  IModel<CodebookFeature> aFeatureModel)
    {
        CodebookFeature feature = aFeatureModel.getObject();
        
        Panel editor;
        switch (feature.getType()) {
        case CAS.TYPE_NAME_INTEGER:
        case CAS.TYPE_NAME_FLOAT:
        case CAS.TYPE_NAME_BOOLEAN:
            editor =  new EmptyPanel(aId);;
            break;
        case CAS.TYPE_NAME_STRING:
            editor = new UimaStringCodebookTraitsEditor(aId, aFeatureModel);
            break;
        default:
            throw unsupportedFeatureTypeException(feature);
        }
        return editor;
    }
    
    @Override
    public void configureCodeBookFeature(CodebookFeature aFeature)
    {
        // If the feature is not a string feature, force the tagset to null.
        if (!(CAS.TYPE_NAME_STRING.equals(aFeature.getType()))) {
            aFeature.setTagset(null);
        }
    }
    
    @Override
    public <T> T getFeatureValue(CodebookFeature aFeature, FeatureStructure aFS)
    {
        Feature feature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        final String effectiveType = aFeature.getType();
        
        // Sanity check
        if (!Objects.equals(effectiveType, feature.getRange().getName())) {
            throw new IllegalArgumentException("Actual feature type ["
                    + feature.getRange().getName() + "] does not match expected feature type ["
                    + effectiveType + "].");
        }

        return WebAnnoCasUtil.getFeature(aFS, aFeature.getName());
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        featureSupportId = aBeanName;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        primitiveTypes = asList(
                new FeatureType(CAS.TYPE_NAME_STRING, "Primitive: String", featureSupportId),
                new FeatureType(CAS.TYPE_NAME_INTEGER, "Primitive: Integer", featureSupportId), 
                new FeatureType(CAS.TYPE_NAME_FLOAT, "Primitive: Float", featureSupportId), 
                new FeatureType(CAS.TYPE_NAME_BOOLEAN, "Primitive: Boolean", featureSupportId));
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            CodebookFeature aFeature) {
        aTD.addFeature(aFeature.getName(), "", aFeature.getType());
        
    }

    @Override
    public List<FeatureType> getPrimitiveFeatureTypes()
    {
        return Collections.unmodifiableList(primitiveTypes);
    }
    
}

