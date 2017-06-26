/*
 * Copyright 2015
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

package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv2Reader;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.conll.Conll2006Reader;
import de.tudarmstadt.ukp.dkpro.core.io.xmi.XmiReader;

public class DiffUtils
{

    public static final String HOST_TYPE = "LinkHost";
    public static final String LINK_TYPE = "LinkType";

    public static Map<String, List<JCas>> load(String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            JCas cas = read(path);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static Map<String, List<JCas>> loadWebAnnoTSV(TypeSystemDescription aTypes,
            String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            JCas cas = readWebAnnoTSV(path, aTypes);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static Map<String, List<JCas>> loadXMI(TypeSystemDescription aTypes, String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            JCas cas = readXMI(path, aTypes);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static JCas read(String aPath)
        throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2006Reader.class,
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);

        JCas jcas = JCasFactory.createJCas();

        reader.getNext(jcas.getCas());

        return jcas;
    }

    public static JCas readWebAnnoTSV(String aPath, TypeSystemDescription aType)
        throws UIMAException, IOException
    {
        CollectionReader reader = createReader(WebannoTsv2Reader.class,
                WebannoTsv2Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        JCas jcas;
        if (aType != null) {
            TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                    .createTypeSystemDescription();
            List<TypeSystemDescription> allTypes = new ArrayList<>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            jcas = JCasFactory.createJCas(CasCreationUtils.mergeTypeSystems(allTypes));
        }
        else {
            jcas = JCasFactory.createJCas();
        }

        reader.getNext(jcas.getCas());

        return jcas;
    }

    public static JCas readXMI(String aPath, TypeSystemDescription aType)
        throws UIMAException, IOException
    {
        CollectionReader reader = createReader(XmiReader.class, XmiReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/" + aPath);
        JCas jcas;
        if (aType != null) {
            TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                    .createTypeSystemDescription();
            List<TypeSystemDescription> allTypes = new ArrayList<>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            jcas = JCasFactory.createJCas(CasCreationUtils.mergeTypeSystems(allTypes));
        }
        else {
            jcas = JCasFactory.createJCas();
        }

        reader.getNext(jcas.getCas());

        return jcas;
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSytem()
        throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();

        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        // Link type
        TypeDescription linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature("target", "", Token.class.getName());

        // Link host
        TypeDescription hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);

        typeSystems.add(tsd);
        typeSystems.add(TypeSystemDescriptionFactory.createTypeSystemDescription());
        TypeSystemDescription allTypes = CasCreationUtils.mergeTypeSystems(typeSystems);

        return allTypes;
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSytem(String... aFeatures)
        throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();

        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        // Link type
        TypeDescription linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature("target", "", Token.class.getName());

        // Link host
        TypeDescription hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
        for (String feature : aFeatures) {
            hostTD.addFeature(feature, "", CAS.TYPE_NAME_STRING);
        }

        typeSystems.add(tsd);
        typeSystems.add(TypeSystemDescriptionFactory.createTypeSystemDescription());
        TypeSystemDescription allTypes = CasCreationUtils.mergeTypeSystems(typeSystems);

        return allTypes;
    }

    public static TypeSystemDescription createCustomTypeSystem(String aType, String aTypeName,
            List<String> aFeatures, String aAttacheType)
        throws Exception
    {
        TypeSystemDescription type = new TypeSystemDescription_impl();
        if (aType.equals(SPAN_TYPE)) {
            TypeDescription td = type.addType(aTypeName, "", CAS.TYPE_NAME_ANNOTATION);
            for (String feature : aFeatures) {
                td.addFeature(feature, "", CAS.TYPE_NAME_STRING);
            }

        }
        else if (aType.equals(RELATION_TYPE)) {
            TypeDescription td = type.addType(aTypeName, "", CAS.TYPE_NAME_ANNOTATION);

            td.addFeature(WebAnnoConst.FEAT_REL_TARGET, "", aAttacheType);
            td.addFeature(WebAnnoConst.FEAT_REL_SOURCE, "", aAttacheType);

            for (String feature : aFeatures) {
                td.addFeature(feature, "", "uima.cas.String");
            }
        }

        return type;
    }

    public static void makeLinkHostFS(JCas aJCas, int aBegin, int aEnd, FeatureStructure... aLinks)
    {
        Type hostType = aJCas.getTypeSystem().getType(HOST_TYPE);
        AnnotationFS hostA1 = aJCas.getCas().createAnnotation(hostType, aBegin, aEnd);
        hostA1.setFeatureValue(hostType.getFeatureByBaseName("links"),
                FSCollectionFactory.createFSArray(aJCas, asList(aLinks)));
        aJCas.getCas().addFsToIndexes(hostA1);
    }

    public static AnnotationFS makeLinkHostMultiSPanFeatureFS(JCas aJCas, int aBegin, int aEnd,
            Feature aSpanFeature, String aValue, FeatureStructure... aLinks)
    {
        Type hostType = aJCas.getTypeSystem().getType(HOST_TYPE);
        AnnotationFS hostA1 = aJCas.getCas().createAnnotation(hostType, aBegin, aEnd);
        hostA1.setFeatureValue(hostType.getFeatureByBaseName("links"),
                FSCollectionFactory.createFSArray(aJCas, asList(aLinks)));
        hostA1.setStringValue(aSpanFeature, aValue);
        aJCas.getCas().addFsToIndexes(hostA1);
        return hostA1;
    }

    public static FeatureStructure makeLinkFS(JCas aJCas, String aSlotLabel, int aTargetBegin,
            int aTargetEnd)
    {
        Token token1 = new Token(aJCas, aTargetBegin, aTargetEnd);
        token1.addToIndexes();

        Type linkType = aJCas.getTypeSystem().getType(LINK_TYPE);
        FeatureStructure linkA1 = aJCas.getCas().createFS(linkType);
        linkA1.setStringValue(linkType.getFeatureByBaseName("role"), aSlotLabel);
        linkA1.setFeatureValue(linkType.getFeatureByBaseName("target"), token1);
        aJCas.getCas().addFsToIndexes(linkA1);

        return linkA1;
    }

}
