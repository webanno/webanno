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
package de.tudarmstadt.ukp.clarin.webanno.csv;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;

public class WebannoCsvWriter extends JCasFileWriter_ImplBase {
    public static final String PARAM_ENCODING = ComponentParameters.PARAM_TARGET_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    public static final String PARAM_CODEBOOKS = "codebooks";
    @ConfigurationParameter(name = PARAM_CODEBOOKS, mandatory = true, defaultValue = {})
    private List<String> codebooks;

    public static final String WITH_HEADERS = "withHeaders";
    @ConfigurationParameter(name = WITH_HEADERS, mandatory = true, defaultValue = "false")
    private boolean withHeaders;

    public static final String WITH_TEXT = "withText";
    @ConfigurationParameter(name = WITH_TEXT, mandatory = true, defaultValue = "false")
    private boolean withText;

    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".csv")
    private String filenameSuffix;

    public static final String PARAM_FILENAME = "filename";
    @ConfigurationParameter(name = PARAM_FILENAME, mandatory = true, defaultValue = "codebooks")
    private String filename;

    public static final String PARAM_ANNOTATOR = "annotator";
    @ConfigurationParameter(name = PARAM_ANNOTATOR, mandatory = true, defaultValue = "adminuser")
    private String annotator;

    private static final String NEW_LINE_SEPARATOR = "\n";

    public static final String DOCUMENT_Name = "documentName";
    @ConfigurationParameter(name = DOCUMENT_Name, mandatory = true, defaultValue = "testDocument.txt")
    private String documentName;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
        OutputStreamWriter writer = null;
        CSVPrinter csvFileWriter = null;
        try {
            CSVFormat csvFileFormat = CSVFormat.RFC4180.withRecordSeparator(NEW_LINE_SEPARATOR);
            File file = new File(filename);
            writer = new OutputStreamWriter(new FileOutputStream(file, true), encoding);
            csvFileWriter = new CSVPrinter(writer, csvFileFormat);
            writeCsv(aJCas, csvFileWriter);
        } catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        } finally {
            try {
                csvFileWriter.flush();
                csvFileWriter.close();
            } catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
            closeQuietly(writer);
        }

    }

    private void writeCsv(JCas aJCas, CSVPrinter aCsvFilePrinter)
            throws IOException, ResourceInitializationException, CASRuntimeException, CASException {

        Set<Type> codebookTypes = new LinkedHashSet<>();
        List<String> headers = new ArrayList<>();

        headers.add("DocumentName");
        headers.add("Annotator");

        for (String t : codebooks) {
            codebookTypes.add(aJCas.getTypeSystem().getType(t));
            headers.add(t);
        }

        if (codebookTypes.isEmpty()) {
            // Nothing to write
            return;
        }
        headers.add("Text");
        if (withHeaders) {
            aCsvFilePrinter.printRecord(headers.toArray(new Object[headers.size()]));
        }
        List<String> codebookValue = new ArrayList<>();
        codebookValue.add(documentName);
        codebookValue.add(annotator);
        
        for (Type codebokType : codebookTypes) {
            for (Feature feature : codebokType.getFeatures()) {
                if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
                        || feature.toString().equals("uima.tcas.Annotation:begin")
                        || feature.toString().equals("uima.tcas.Annotation:end")) {
                    continue;
                }
                if (CasUtil.select(aJCas.getCas(), codebokType).isEmpty()) {
                    codebookValue.add(new String());
                } else {
                    codebookValue.add(CasUtil.select(aJCas.getCas(), codebokType).iterator().next()
                            .getFeatureValueAsString(feature));
                }
            }
        }
        // String someText = aJCas.getDocumentText().substring(0, Math.min(50,
        // aJCas.getDocumentText().length()));
        if (withText) {
            codebookValue.add(aJCas.getDocumentText());
        }
        aCsvFilePrinter.printRecord(codebookValue);
    }

}
