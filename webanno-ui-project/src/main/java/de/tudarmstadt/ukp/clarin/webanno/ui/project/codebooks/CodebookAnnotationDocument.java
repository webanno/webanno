
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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.codebooks;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Represent each line of the CSV codebook annotation from a file system to
 * import the codebook annotations during file import
 *
 */
public class CodebookAnnotationDocument {

    String text;
    
    String documentName;
    
    List<String> annotators = new ArrayList<>();

    List<List<String>> codebooks = new ArrayList<>();

    List<String> headers = new ArrayList<>();
    
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getAnnotators() {
        return annotators;
    }

    public void setAnnotators(List<String> annotators) {
        this.annotators = annotators;
    }

    public List<List<String>> getCodebooks() {
        return codebooks;
    }

    public void setCodebooks(List<List<String>> codebooks) {
        this.codebooks = codebooks;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((documentName == null) ? 0 : documentName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CodebookAnnotationDocument other = (CodebookAnnotationDocument) obj;
        if (documentName == null) {
            if (other.documentName != null)
                return false;
        } else if (!documentName.equals(other.documentName))
            return false;
        return true;
    }

    
}
