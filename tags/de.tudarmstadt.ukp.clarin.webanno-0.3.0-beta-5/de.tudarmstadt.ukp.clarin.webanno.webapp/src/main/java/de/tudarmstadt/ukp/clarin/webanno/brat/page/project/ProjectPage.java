/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.page.project;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wicket.Component;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * This is the main page for Project Settings. The Page has Four Panels. The
 * {@link AnnotationGuideLinePanel} is used to update documents to a project. The
 * {@link ProjectDetailsPanel} used for updating Project deatils such as descriptions of a project
 * and name of the Project The {@link ProjectTagSetsPanel} is used to add {@link TagSet} and
 * {@link Tag} details to a Project as well as updating them The {@link ProjectUsersPanel} is used
 * to update {@link User} to a Project
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class ProjectPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(ProjectPage.class);

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private Button creatProject;
        private ListChoice<Project> projects;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(creatProject = new Button("create", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    ProjectSelectionForm.this.getModelObject().project = null;
                    projectDetailForm.setModelObject(new Project());
                    createProject = true;
                    projectDetailForm.setVisible(true);
                    ProjectSelectionForm.this.setVisible(true);
                }
            });

            MetaDataRoleAuthorizationStrategy.authorize(creatProject, Component.RENDER,
                    "ROLE_ADMIN");

            add(projects = new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            List<Project> allowedProject = new ArrayList<Project>();

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            List<Project> allProjects = projectRepository.listProjects();
                            List<Authority> authorities = projectRepository.getAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getRole().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ApplicationUtils.isProjectAdmin(project, projectRepository,
                                        user)) {
                                    allowedProject.add(project);
                                }
                                else {
                                    error("You don't have permission!");
                                }
                            }
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    if (aNewSelection != null) {
                        createProject = false;
                        int selectedTab = projectDetailForm.allTabs.getSelectedTab();
                        if(selectedTab<0) {
                            selectedTab=0;
                        }
                        updateProjectDetailForm();
                        projectDetailForm.setModelObject(aNewSelection);
                        projectDetailForm.setVisible(true);
                        projectDetailForm.allTabs.setSelectedTab(selectedTab);

                        ProjectSelectionForm.this.setVisible(true);
                    }
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
        }
    }

    private void updateProjectDetailForm(){
        remove(projectDetailForm);
        projectDetailForm = new ProjectDetailForm("projectDetailForm");
        //projectDetailForm.ge
        add(projectDetailForm);
    }
    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
        private List<String> documents;
        private List<String> permissionLevels;
        private User user;
    }

    private class ProjectDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        AbstractTab details;
        AbstractTab users;
        AbstractTab tagSets;
        AbstractTab documents;
        AjaxTabbedPanel allTabs;

        public ProjectDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

            List<ITab> tabs = new ArrayList<ITab>();
            tabs.add(details = new AbstractTab(new Model<String>("Project Details"))
            {
                private static final long serialVersionUID = 6703144434578403272L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectDetailsPanel(panelId);
                }

                @Override
                public boolean isVisible()
                {
                    return true;

                }

            });

            tabs.add(users = new AbstractTab(new Model<String>("Project Users"))
            {
                private static final long serialVersionUID = 7160734867954315366L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectUsersPanel(panelId, projectDetailForm.getModelObject());
                }

                @Override
                public boolean isVisible()
                {
                    if (createProject) {
                        return false;
                    }
                    return true;

                }
            });

            tabs.add(documents = new AbstractTab(new Model<String>("Project Documents"))
            {
                private static final long serialVersionUID = 1170760600317199418L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectDocumentsPanel(panelId, projectDetailForm.getModelObject());
                }

                @Override
                public boolean isVisible()
                {
                    if (createProject) {
                        return false;
                    }
                    return true;

                }
            });

            tabs.add(tagSets = new AbstractTab(new Model<String>("Project TagSets"))
            {
                private static final long serialVersionUID = -3205723896786674220L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new ProjectTagSetsPanel(panelId, projectDetailForm.getModelObject());
                }

                @Override
                public boolean isVisible()
                {
                    if (createProject) {
                        return false;
                    }
                    return true;

                }
            });

            tabs.add(new AbstractTab(new Model<String>("Annotation Guideline"))
            {
                private static final long serialVersionUID = 7887973231065189200L;

                @Override
                public Panel getPanel(String panelId)
                {
                    return new AnnotationGuideLinePanel(panelId, projectDetailForm.getModelObject());
                }

                @Override
                public boolean isVisible()
                {
                    if (createProject) {
                        return false;
                    }
                    return true;

                }
            });

            add(allTabs = new AjaxTabbedPanel("tabs", tabs));
            ProjectDetailForm.this.setMultiPart(true);
        }
    }

    private ProjectSelectionForm projectSelectionForm;
    private ProjectDetailForm projectDetailForm;
    // Fix for Issue "refresh for "new project" in project configuration (Bug #141) "
    boolean createProject = false;

    public ProjectPage()
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        projectDetailForm = new ProjectDetailForm("projectDetailForm");
        projectDetailForm.setVisible(false);

        add(projectSelectionForm);
        add(projectDetailForm);
    }

    private class ProjectDetailsPanel
        extends Panel
    {
        private static final long serialVersionUID = 1118880151557285316L;

        public ProjectDetailsPanel(String id)
        {
            super(id);
            add(new TextField<String>("name").setRequired(true));

            add(new TextArea<String>("description").setOutputMarkupPlaceholderTag(true));
            // Add check box to enable/disable arc directions of dependency parsing
            add(new CheckBox("reverseDependencyDirection"));
            add(new Button("save", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    boolean projectExist = false;
                    try {
                        projectRepository.existsProject(project.getName());
                    }
                    catch (Exception e) {
                        error("Another project with name [" + project.getName() + "] exists!"
                                + ExceptionUtils.getRootCauseMessage(e));
                        projectExist = true;
                    }
                    // If only the project is new!
                    if (project.getId() == 0 && !projectExist) {
                        // Check if the project with this name already exist
                        if (projectRepository.existsProject(project.getName())) {
                            error("Project with this name already exist !");
                            LOG.error("Project with this name already exist !");
                        }
                        else if (isProjectNameValid(project.getName())) {
                            try {
                                String username = SecurityContextHolder.getContext()
                                        .getAuthentication().getName();
                                User user = projectRepository.getUser(username);
                                projectRepository.createProject(project, user);
                                annotationService.initializeTypesForProject(project, user);
                                projectDetailForm.setVisible(true);
                            }
                            catch (IOException e) {
                                error("Project repository path not found " + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                                LOG.error("Project repository path not found " + ":"
                                        + ExceptionUtils.getRootCauseMessage(e));
                            }
                        }
                        else {
                            error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                            LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        }
                    }
                    // This is updating Project details
                    else {
                        // Invalid Project name, restore
                        if (!isProjectNameValid(project.getName()) && !projectExist) {

                            // Maintain already loaded project and selected Users
                            // Hence Illegal Project modification (limited privilege, illegal
                            // project
                            // name,...) preserves the original one

                            String oldProjectName = projectRepository.getProject(project.getId())
                                    .getName();
                            List<User> selectedusers = projectRepository.listProjectUsers(project);

                            project.setName(oldProjectName);
                            project.setUsers(new HashSet<User>(selectedusers));
                            error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                            LOG.error("Project name shouldn't contain characters such as /\\*?&!$+[^]");
                        }
                    }
                    createProject = false;
                }
            });
            add(new Button("remove", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    Project project = projectDetailForm.getModelObject();
                    if (project.getId() != 0) {
                        try {
                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = projectRepository.getUser(username);

                            projectRepository.removeProject(projectDetailForm.getModelObject(),
                                    user);
                            projectDetailForm.setVisible(false);
                        }
                        catch (IOException e) {
                            LOG.error("Unable to remove project :"
                                    + ExceptionUtils.getRootCauseMessage(e));
                            error("Unable to remove project " + ":"
                                    + ExceptionUtils.getRootCauseMessage(e));
                        }

                    }
                }
            });
        }

        /**
         * Check if the Project name is valid, SPecial characters are not allowed as a project name
         * as it will conflict with file naming system
         *
         * @param aProjectName
         * @return
         */
        public boolean isProjectNameValid(String aProjectName)
        {
            if (aProjectName.contains("^") || aProjectName.contains("/")
                    || aProjectName.contains("\\") || aProjectName.contains("&")
                    || aProjectName.contains("*") || aProjectName.contains("?")
                    || aProjectName.contains("+") || aProjectName.contains("$")
                    || aProjectName.contains("!") || aProjectName.contains("[")
                    || aProjectName.contains("]")) {
                return false;
            }
            else {
                return true;
            }
        }
    }
}
