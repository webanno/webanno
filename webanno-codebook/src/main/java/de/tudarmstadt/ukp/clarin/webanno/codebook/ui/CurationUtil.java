package de.tudarmstadt.ukp.clarin.webanno.codebook.ui;

import java.util.LinkedList;
import java.util.List;

import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.CodebookAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.Codebook;

public class CurationUtil {

    public final static String CURATION_USER = "CURATION_USER";
 
    public static List<Type> getCodebookTypes(JCas mergeJCas, List<Codebook> aCodebooks) {
        List<Type> entryTypes = new LinkedList<>();

        for (Codebook codebook : aCodebooks) {
            CodebookAdapter cA = new CodebookAdapter(codebook);
            entryTypes.add(cA.getAnnotationType(mergeJCas.getCas()));
        }
        return entryTypes;
    }

}
