/*
 * Copyright 2020
 * Ubiquitous Knowledge Processing (UKP) Lab Technische Universität Darmstadt
 * and  Language Technology Universität Hamburg
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
package de.tudarmstadt.ukp.clarin.webanno.codebook.ui.agreement;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.googlecode.wicket.kendo.ui.markup.html.link.Link;

import de.tudarmstadt.ukp.clarin.webanno.codebook.CodebookConst;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.Codebook;
import de.tudarmstadt.ukp.clarin.webanno.codebook.model.CodebookNode;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookNodeExpansion;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookTreePanel;
import de.tudarmstadt.ukp.clarin.webanno.codebook.ui.tree.CodebookTreeProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class AgreementCodebookTreePanel
    extends CodebookTreePanel
{
    private static final long serialVersionUID = -3054236611676847524L;

    private Project project;
    private CodebookAgreementPage parentPage;
    private CodebookNode selected;

    public AgreementCodebookTreePanel(String aId, IModel<?> aModel, Project project,
            CodebookAgreementPage parentPage)
    {
        super(aId, aModel);
        this.project = project;
        this.parentPage = parentPage;

        // create and add expand and collapse links
        this.add(new Link<Void>("expandAll")
        {
            private static final long serialVersionUID = -2081711094768955973L;

            public void onClick()
            {
                CodebookNodeExpansion.get().expandAll();
            }
        });
        this.add(new Link<Void>("collapseAll")
        {
            private static final long serialVersionUID = -4576757597732733009L;

            public void onClick()
            {
                CodebookNodeExpansion.get().collapseAll();
            }
        });
    }

    private AnnotationFeature createWrapperAnnotationFeature()
    {
        if (selected == null)
            return null;

        AnnotationLayer neLayer = new AnnotationLayer(selected.getName(), selected.getUiName(),
                SPAN_TYPE, project, false, TOKENS, ANY_OVERLAP);

        return new AnnotationFeature(project, neLayer, CodebookConst.CODEBOOK_FEATURE_NAME,
                CodebookConst.CODEBOOK_FEATURE_NAME, CAS.TYPE_NAME_STRING,
                selected.getCodebook().getDescription(),
                null);
    }

    @Override
    public void initTree()
    // TODO actually we can implement this method in the CodebookTreePanel superclass
    // and only provide a callback for newContentComponent(.) and pass the treeID
    // in the ctor of CodebookTreePanel ...
    {
        this.initCodebookNodeProvider();
        tree = new NestedTree<CodebookNode>("agreementCodebookTree", this.provider,
                new CodebookNodeExpansionModel())
        {
            private static final long serialVersionUID = 2285250157811357702L;

            @Override
            protected Component newContentComponent(String id, IModel<CodebookNode> model)
            {
                return buildFolderComponent(id, model);
            }

        };

        this.applyTheme();
        tree.setOutputMarkupId(true);
        tree.add(new Behavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(Component component, ComponentTag tag)
            {
                theme.onComponentTag(component, tag);
            }

            @Override
            public void renderHead(Component component, IHeaderResponse response)
            {
                theme.renderHead(component, response);
            }
        });
        this.addOrReplace(tree);
    }

    @Override
    public void initCodebookNodeProvider()
    {
        // get all codebooks and init the provider
        List<Codebook> codebooks = this.codebookService.listCodebook(this.project);
        this.provider = new CodebookTreeProvider(codebooks);
    }

    // TODO we could also put this into CodebookTreePanel and make the onClick callback abstract
    private Folder<CodebookNode> buildFolderComponent(String id, IModel<CodebookNode> model)
    {
        Folder<CodebookNode> folder = new Folder<CodebookNode>(id, tree, model)
        {

            private static final long serialVersionUID = -142180741827668758L;

            @Override
            protected boolean isClickable()
            {
                return true;
            }

            @Override
            protected boolean isSelected()
            {
                CodebookNode node = this.getModelObject();
                return node != null && node.isSelected();
            }

            @Override
            protected void onClick(Optional<AjaxRequestTarget> targetOptional)
            {

                AgreementCodebookTreePanel.this.selected = this.getModelObject();

                AjaxRequestTarget _target = targetOptional.get();

                // create a wrapper annotation feature from the selected codebook and select the
                // annotation feature in the agreement form
                parentPage.agreementForm.actionSelectFeature(_target,
                        createWrapperAnnotationFeature());

                // TODO for some reason, highlighting the node the way I try won't work..
                highlightNode(_target, this.getModelObject(), true);

                if (!CodebookNodeExpansion.get().contains(this.getModelObject())) {
                    CodebookNodeExpansion.get().add(this.getModelObject());
                    tree.expand(this.getModelObject());
                }
                else {
                    CodebookNodeExpansion.get().remove(this.getModelObject());
                    tree.collapse(this.getModelObject());
                }
            }

            private void highlightNode(AjaxRequestTarget target, CodebookNode selectedNode,
                    boolean activate)
            {
                List<Component> treeNodeList = this.streamChildren()
                        .filter(component -> component instanceof Label
                                && component.getDefaultModelObject().equals(selectedNode))
                        .collect(Collectors.toList());

                // add or remove the highlighted class..
                if (treeNodeList.size() == 1) {
                    treeNodeList.get(0).setOutputMarkupId(true);
                    treeNodeList.get(0).getParent().setOutputMarkupId(true);
                    if (activate)
                        treeNodeList.get(0).getParent()
                                .add(AttributeAppender.append("class", "highlighted"));
                    else
                        treeNodeList.get(0).getParent()
                                .add(new AttributeModifier("class", new Model<>("highlighted"))
                                {
                                    private static final long serialVersionUID = -1L;

                                    @Override
                                    protected String newValue(String currentValue,
                                            String valueToRemove)
                                    {
                                        return currentValue.replaceAll(valueToRemove, "");
                                    }
                                });

                    target.add(AgreementCodebookTreePanel.this);
                    target.add(tree);
                    target.add(treeNodeList.get(0).getParent());
                    target.add(treeNodeList.get(0));
                }

            }

        };

        return folder;
    }

    public void setProject(Project project)
    {
        this.project = project;
        this.initCodebookNodeProvider();
        this.initTree();
    }

    // package private by intention
    void expandAll()
    {
        CodebookNodeExpansion.get().expandAll();
    }

    // package private by intention
    void collapseAll()
    {
        CodebookNodeExpansion.get().collapseAll();
    }
}
