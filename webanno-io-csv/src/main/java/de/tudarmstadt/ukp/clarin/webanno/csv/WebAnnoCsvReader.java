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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

public class WebAnnoCsvReader
    extends JCasResourceCollectionReader_ImplBase
{

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        try (InputStream is = new BufferedInputStream(res.getInputStream())) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withIgnoreHeaderCase()
                    .parse(new InputStreamReader(is, "UTF-8"));
            List<String> headers = new ArrayList<>();
            for (CSVRecord record : records) {
                if (headers.isEmpty()) {
                    String documentName = record.get(0);
                    String annotator = record.get(1);

                    headers.add(documentName);
                    headers.add(annotator);
                    for (int c = 2; c < record.size(); c++) {
                        headers.add(record.get(c));
                    }
                }
                else {
                    String text = record.get(record.size() - 1);
                    if (null != text && null == aJCas.getDocumentText()) {
                        aJCas.setDocumentText(text);
                    }
                    // add the codebook annotations
                    // TODO this needs to get generified for every type of (custom) annotation if we
                    // TODO want CSV support not only for Codebook
                    createCodebookAnnotations(aJCas, headers, record);

                }

            }
        }
        catch (Exception e) {
            throw new CollectionException(e);
        }

    }

    private void createCodebookAnnotations(JCas aJCas, List<String> headers, CSVRecord record)
        throws IOException
    {
        AnnotationFS codebookAnnotation;
        // start with 2 since the first and second header entries are the document name and
        // annotator name respectively. Also the last header entry (text of the document) is
        // ignored.
        for (int i = 2; i < record.size() - 1; i++) {
            Type codebook = aJCas.getTypeSystem().getType(headers.get(i));
            if (codebook == null)
                throw new IOException("Codebook Type with name '" + headers.get(i)
                        + "' is not registered in the typesystem!");
            codebookAnnotation = aJCas.getCas().createAnnotation(codebook, 0,
                    aJCas.getDocumentText().length() - 1);
            codebookAnnotation.setFeatureValueFromString(codebook.getFeatureByBaseName("code"),
                    record.get(i));
            aJCas.addFsToIndexes(codebookAnnotation);
        }
    }

}
