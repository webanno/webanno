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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.BooleanFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.InputFieldTextFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoAutoCompleteTextFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoComboboxTextFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.NumberFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.UimaStringTraitsEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class PrimitiveUimaFeatureSupport
    implements FeatureSupport<Void>, InitializingBean
{
    private List<FeatureType> primitiveTypes;

    private String featureSupportId;
    
    @Override
    public String getId()
    {
        return featureSupportId;
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
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return Collections.unmodifiableList(primitiveTypes);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            switch (aFeature.getType()) {
            case CAS.TYPE_NAME_INTEGER: // fallthrough
            case CAS.TYPE_NAME_FLOAT: // fallthrough
            case CAS.TYPE_NAME_BOOLEAN: // fallthrough
            case CAS.TYPE_NAME_STRING: 
                return true;
            default:
                return false;
            }
        case ARRAY: // fallthrough
        default:
            return false;
        }
    }
    
    @Override
    public Panel createTraitsEditor(String aId,  IModel<AnnotationFeature> aFeatureModel)
    {
        AnnotationFeature feature = aFeatureModel.getObject();
        
        Panel editor;
        switch (feature.getMultiValueMode()) {
        case NONE:
            switch (feature.getType()) {
            case CAS.TYPE_NAME_INTEGER:
            case CAS.TYPE_NAME_FLOAT:
            case CAS.TYPE_NAME_BOOLEAN:
                editor = FeatureSupport.super.createTraitsEditor(aId, aFeatureModel);
                break;
            case CAS.TYPE_NAME_STRING:
                editor = new UimaStringTraitsEditor(aId, aFeatureModel);
                break;
            default:
                throw unsupportedFeatureTypeException(feature);
            }
            break;
        case ARRAY: // fall-through
            throw unsupportedLinkModeException(feature);
        default:
            throw unsupportedMultiValueModeException(feature);
        }
        return editor;
    }
    
    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;
        final FeatureEditor editor;
        
        switch (feature.getMultiValueMode()) {
        case NONE:
            switch (feature.getType()) {
            case CAS.TYPE_NAME_INTEGER: {
                editor = new NumberFeatureEditor(aId, aOwner, aFeatureStateModel);
                break;
            }
            case CAS.TYPE_NAME_FLOAT: {
                editor = new NumberFeatureEditor(aId, aOwner, aFeatureStateModel);
                break;
            }
            case CAS.TYPE_NAME_BOOLEAN: {
                editor = new BooleanFeatureEditor(aId, aOwner, aFeatureStateModel);
                break;
            }
            case CAS.TYPE_NAME_STRING: {
                if (feature.getTagset() == null) {
                    // If there is no tagset, use a simple input field
                    editor = new InputFieldTextFeatureEditor(aId, aOwner, aFeatureStateModel);
                }
                else if (aFeatureStateModel.getObject().tagset.size() < 75) {
                    // For smaller tagsets, use a combobox
                    editor = new KendoComboboxTextFeatureEditor(aId, aOwner, aFeatureStateModel);
                }
                else {
                    // For larger ones, use an auto-complete field
                    editor = new KendoAutoCompleteTextFeatureEditor(aId, aOwner,
                            aFeatureStateModel);
                }
                break;
            }
            default:
                throw unsupportedFeatureTypeException(feature);
            }
            break;
        case ARRAY: // fall-through
            throw unsupportedLinkModeException(feature);
        default:
            throw unsupportedMultiValueModeException(feature);
        }
        return editor;
    }
    
    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), "", aFeature.getType());
    }
    
    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        // If the feature is not a string feature, force the tagset to null.
        if (!(CAS.TYPE_NAME_STRING.equals(aFeature.getType()))) {
            aFeature.setTagset(null);
        }
    }
    
    @Override
    public boolean isTagsetSupported(AnnotationFeature aFeature)
    {
        // Only string features support tagsets
        return CAS.TYPE_NAME_STRING.equals(aFeature.getType());
    }
    
    @Override
    public <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
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
}
