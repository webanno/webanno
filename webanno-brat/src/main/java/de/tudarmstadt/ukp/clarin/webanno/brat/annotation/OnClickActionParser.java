package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * 
 *
 */
public class OnClickActionParser implements Serializable {
	
	private static final long serialVersionUID = 3008662322929838450L;
	
	private final static Logger LOG = LoggerFactory.getLogger(OnClickActionParser.class); 

	/**
	 * 
	 * @param jstemplate in the form of e.g. "alert('${PID} ${DID} ${DNAME} ${ID}')"
	 * @param project
	 * @param document
	 * @param anno
	 * @return String with substituted variables
	 */
	public String parse(String jstemplate, Project project, SourceDocument document, AnnotationFS anno){
		Map<String, String> valuesMap = new HashMap<>();
		valuesMap.put("PID", String.valueOf(project.getId()));
		valuesMap.put("PNAME", project.getName());
		valuesMap.put("DOCID", String.valueOf(document.getId()));
		valuesMap.put("DOCNAME", document.getName());

		// collect the methods of the annotation
		Method[] methods = anno.getClass().getDeclaredMethods();
		Arrays.stream(methods)
			.filter(m -> m.getParameterCount() < 1 && m.getReturnType() != Void.TYPE) // only select getter methods with no parameters and a return type
			.forEach(m -> {
				if(m.getName().startsWith("get")){
					String value = getStringValue(m, anno);
					if(value != null){
						String subst = m.getName().substring(3); // just cut-off 'get'
						valuesMap.put(subst, value);
					}
					return;
				}
				if(m.getName().startsWith("is")){
					String value = getStringValue(m, anno);
					if(value != null){
						String subst = m.getName(); // don't cut-off 'is'
						valuesMap.put(subst, value);
					}
					return;
				}
				// else nothing to add
			});
		StrSubstitutor sub = new StrSubstitutor(valuesMap);
		String js = sub.replace(jstemplate);
		return js;
	}
	
	String getStringValue(Method method, Object obj){
		try {
			Object result = method.invoke(obj);
			if(result != null)
				return String.valueOf(result);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOG.debug(String.format("Some %s error happened, but probably not an important one: '%s'.", e.getClass(), e.getMessage()), e);
			return null;
		}
		return null;
	}

}
