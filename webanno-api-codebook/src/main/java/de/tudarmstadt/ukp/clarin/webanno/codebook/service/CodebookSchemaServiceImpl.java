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
package de.tudarmstadt.ukp.clarin.webanno.codebook.service;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookCategory;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookFeature;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookTag;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;

/**
 * Implementation of methods defined in the {@link AnnotationSchemaService}
 * interface
 */
@Component(CodebookSchemaService.SERVICE_NAME)
public class CodebookSchemaServiceImpl implements CodebookSchemaService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    

    private @PersistenceContext EntityManager entityManager;
    // private @Lazy @Autowired(required = false) List<ProjectInitializer>
    // initializerProxy;
    private @Autowired CodebookFeatureSupportRegistry featureSupportRegistry;

    public CodebookSchemaServiceImpl() {
       // Nothing to do
    }

    @Override
    @Transactional
    public void createCodebook(Codebook codebook) {
        if (isNull(codebook.getId())) {
            entityManager.persist(codebook);
        } else {
            entityManager.merge(codebook);
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(codebook.getProject().getId()))) {
            Project project = codebook.getProject();
            log.info("Created codebook [{}]({}) in project [{}]({})", codebook.getName(),
                    codebook.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void createCodebookFeature(CodebookFeature aFeature) {
        if (isNull(aFeature.getId())) {
            entityManager.persist(aFeature);
        } else {
            entityManager.merge(aFeature);
        }
    }

    @Override
    @Transactional
    public void createCodebookCategory(CodebookCategory aCategory) {
        if (isNull(aCategory.getId())) {
            entityManager.persist(aCategory);
        } else {
            entityManager.merge(aCategory);
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aCategory.getProject().getId()))) {
            Project project = aCategory.getProject();
            log.info("Created codebook_category [{}]({}) in project [{}]({})", aCategory.getName(),
                    aCategory.getId(), project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public boolean existsFeature(String aName, Codebook aCodebook) {

        try {
            entityManager
                    .createQuery("FROM CodebookFeature WHERE name = :name AND codebook = :codebook",
                            CodebookFeature.class)
                    .setParameter("name", aName).setParameter("codebook", aCodebook)
                    .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;

        }
    }

    @Override
    public boolean existsCodebookTag(String aTagName, CodebookCategory aCategory) {
        try {
            getCodebookTag(aTagName, aCategory);
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public CodebookTag getCodebookTag(String aTagName, CodebookCategory aCategory) {
        return entityManager
                .createQuery("FROM CodebookTag WHERE name = :name AND" + " category =:category",
                        CodebookTag.class)
                .setParameter("name", aTagName).setParameter("category", aCategory)
                .getSingleResult();
    }

    @Override
    @Transactional(noRollbackFor = NoResultException.class)
    public boolean existsCodebook(String aName, Project aProject) {
        try {
            entityManager
                    .createQuery("FROM Codebook WHERE name = :name AND project = :project",
                            Codebook.class)
                    .setParameter("name", aName).setParameter("project", aProject)
                    .getSingleResult();
            return true;
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public Codebook getCodebook(long aId)
    {
        return entityManager.createQuery("FROM Codebook WHERE id = :id", Codebook.class)
                .setParameter("id", aId).getSingleResult();
    }

    @Override
    @Transactional
    public Codebook getCodebook(int aCodebookorder, Project aProject)
    {
        return entityManager
                .createQuery(
                        "FROM Codebook WHERE codebookorder = :codebookorder AND project =:project ",
                        Codebook.class)
                .setParameter("codebookorder", aCodebookorder).setParameter("project", aProject)
                .getSingleResult();
    }

    @Override
    @Transactional
    public Codebook getCodeBook(String aName, Project aProject) {
        return entityManager
                .createQuery("From Codebook where name = :name AND project =:project",
                        Codebook.class)
                .setParameter("name", aName).setParameter("project", aProject).getSingleResult();
    }

    @Override
    @Transactional
    public CodebookFeature getCodebookFeature(String aName, Codebook aCodebook)
    {
        return entityManager
                .createQuery("FROM CodebookFeature WHERE name = :name AND codebook = :codebook",
                        CodebookFeature.class)
                .setParameter("name", aName).setParameter("codebook", aCodebook).getSingleResult();
    }

    @Override
    @Transactional
    public void removeCodebookCategory(CodebookCategory aCategory) 
    {
        for (CodebookTag tag : this.listTags(aCategory)) {
            entityManager.remove(tag);
        }
        entityManager.remove(
                entityManager.contains(aCategory) ? aCategory : entityManager.merge(aCategory));
    }

    @Override
    @Transactional
    public void createCodebookTag(CodebookTag aTag) {
        if (isNull(aTag.getId())) {
            entityManager.persist(aTag);
        } else {
            entityManager.merge(aTag);
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aTag.getCategory().getProject().getId()))) {
            CodebookCategory category = aTag.getCategory();
            Project project = category.getProject();
            log.info(
                    "Created codebook_tag [{}]({}) in codebook_category [{}]({}) in project [{}]({})",
                    aTag.getName(), aTag.getId(), category.getName(), category.getId(),
                    project.getName(), project.getId());
        }
    }

    @Override
    @Transactional
    public void removeCodebookTag(CodebookTag aTag) {
        entityManager.remove(entityManager.contains(aTag) ? aTag : entityManager.merge(aTag));
    }

    @Override
    @Transactional
    public List<CodebookTag> listTags(CodebookCategory aCategory) {
        return entityManager
                .createQuery("FROM CodebookTag WHERE category = :category ORDER BY name ASC",
                        CodebookTag.class)
                .setParameter("category", aCategory).getResultList();
    }

    @Override
    @Transactional
    public List<Codebook> listCodebook(Project aProject) {
        return entityManager
                .createQuery("FROM Codebook WHERE project =:project ORDER BY codebookorder asc",
                        Codebook.class)
                .setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public List<CodebookFeature> listCodebookFeature(Codebook code) {
        if (isNull(code) || isNull(code.getId())) {
            return new ArrayList<>();
        }

        return entityManager
                .createQuery("FROM CodebookFeature  WHERE codebook =:codebook ORDER BY uiName",
                        CodebookFeature.class)
                .setParameter("codebook", code).getResultList();
    }

    @Override
    @Transactional
    public List<CodebookFeature> listCodebookFeature(Project aProject)
    {
        return entityManager.createQuery(
                "FROM CodebookFeature f WHERE project =:project ORDER BY f.codebook.uiName, f.uiName",
                CodebookFeature.class).setParameter("project", aProject).getResultList();
    }

    @Override
    @Transactional
    public void removeCodebook(Codebook aCodebook) {
        for (CodebookFeature feature : listCodebookFeature(aCodebook)) {

            removeCodebookFeature(feature);
        }

        entityManager.remove(
                entityManager.contains(aCodebook) ? aCodebook : entityManager.merge(aCodebook));
    }

    public void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD,
            Codebook aCodebook) {
        List<CodebookFeature> features = listCodebookFeature(aCodebook);
        for (CodebookFeature feature : features) {
            CodebookFeatureSupport fs = featureSupportRegistry.getFeatureSupport(feature);
            fs.generateFeature(aTSD, aTD, feature);
        }
    }

    @Override
    public void removeCodebookFeature(CodebookFeature aFeature)
    {
        entityManager.remove(
                entityManager.contains(aFeature) ? aFeature : entityManager.merge(aFeature));

    }

}
