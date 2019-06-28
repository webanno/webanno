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
package de.tudarmstadt.ukp.clarin.webanno.codebook;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.service.CodebookSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTag;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedTagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;

public class ImportUtil {
    public static de.tudarmstadt.ukp.clarin.webanno.codebook.export.ExportedCodebook exportCodebook(
            Codebook aCodebook, AnnotationSchemaService aAnnotationService,
            CodebookSchemaService aCodebookService) {
        de.tudarmstadt.ukp.clarin.webanno.codebook.export.ExportedCodebook exLayer = 
                new de.tudarmstadt.ukp.clarin.webanno.codebook.export.ExportedCodebook();
        exLayer.setDescription(aCodebook.getDescription());
        exLayer.setName(aCodebook.getName());
        exLayer.setProjectName(aCodebook.getProject().getName());
        exLayer.setUiName(aCodebook.getUiName());

        List<de.tudarmstadt.ukp.clarin.webanno.codebook.export.ExportedCodebookFeature> exFeatures = 
                new ArrayList<>();
        for (CodebookFeature feature : aCodebookService.listCodebookFeature(aCodebook)) {
            de.tudarmstadt.ukp.clarin.webanno.codebook.export.ExportedCodebookFeature exFeature = 
                    new de.tudarmstadt.ukp.clarin.webanno.codebook.export.ExportedCodebookFeature();
            exFeature.setDescription(feature.getDescription());
            exFeature.setName(feature.getName());
            exFeature.setProjectName(feature.getProject().getName());
            exFeature.setType(feature.getType());
            exFeature.setUiName(feature.getUiName());
            
            if (feature.getTagset() != null) {
                TagSet tagSet = feature.getTagset();
                ExportedTagSet exTagSet = 
                        new ExportedTagSet();
                exTagSet.setDescription(tagSet.getDescription());
                exTagSet.setLanguage(tagSet.getLanguage());
                exTagSet.setName(tagSet.getName());
                exTagSet.setCreateTag(tagSet.isCreateTag());

                List<ExportedTag> exportedTags = 
                        new ArrayList<>();
                for (Tag tag : aAnnotationService.listTags(tagSet)) {
                    ExportedTag exTag = 
                            new ExportedTag();
                    exTag.setDescription(tag.getDescription());
                    exTag.setName(tag.getName());
                    exportedTags.add(exTag);
                }
                exTagSet.setTags(exportedTags);
                exFeature.setTagSet(exTagSet);
            }
            exFeatures.add(exFeature);
        }
        exLayer.setFeatures(exFeatures);
        return exLayer;
    }
}
