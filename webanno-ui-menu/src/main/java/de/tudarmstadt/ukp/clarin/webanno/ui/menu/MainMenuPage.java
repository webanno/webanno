/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.ui.menu;

import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.annotationEnabeled;
import static de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil.curationEnabeled;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.StatelessLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.resource.UrlResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemService;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItemService.MenuItemDecl;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

/**
 * Main menu page.
 */
public class MainMenuPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -2487663821276301436L;

    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean MenuItemService menuItemService;

    private ListView<MenuItemDecl> menu;

    public MainMenuPage()
    {
        setStatelessHint(true);
        setVersioned(false);
        
        // In case we restore a saved session, make sure the user actually still exists in the DB.
        User user = null;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            user = userRepository.get(username);
        }
        // redirect to login page (if no usr is found, admin/admin will be created)
        catch (NoResultException e) {
            setResponsePage(LoginPage.class);
        }
        
        // if not either a curator or annotator, display warning message
        if (
                !annotationEnabeled(projectService, user, WebAnnoConst.PROJECT_TYPE_ANNOTATION)
                && !annotationEnabeled(projectService, user, WebAnnoConst.PROJECT_TYPE_AUTOMATION)
                && !annotationEnabeled(projectService, user, WebAnnoConst.PROJECT_TYPE_CORRECTION)
                && !curationEnabeled(projectService, user)) 
        {
            info("You are not member of any projects to annotate or curate");
        }
        
        List<MenuItemDecl> menuItems = new ArrayList<>(menuItemService.getMenuItems());
        
        menu = new ListView<MenuItemDecl>("menu", menuItems)
        {
            private static final long serialVersionUID = -5492972164756003552L;

            @Override
            protected void populateItem(ListItem<MenuItemDecl> aItem)
            {
                MenuItemDecl item = aItem.getModelObject();
                StatelessLink<Void> menulink = new StatelessLink<Void>("menulink") {
                    private static final long serialVersionUID = 4110674757822252390L;

                    @Override
                    public void onClick()
                    {
                        setResponsePage(item.page);
                    }
                };
                menulink.add(new Image("icon", new UrlResourceReference(Url.parse(item.icon))));
                menulink.add(new Label("label", PropertyModel.of(item, "label")));
                if (item.condition != null) {
                    menulink.setVisible(item.condition.applies());
                }
                aItem.add(menulink);
            }
        };
        add(menu);
    }
}
