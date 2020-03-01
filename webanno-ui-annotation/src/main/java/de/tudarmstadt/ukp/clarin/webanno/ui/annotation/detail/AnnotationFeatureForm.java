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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.handleException;
import static java.util.Objects.isNull;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;
import static org.apache.wicket.util.string.Strings.escapeMarkup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxPreventSubmitBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.kendo.ui.form.TextField;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.LinkFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.FeatureEditorValueChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.event.LinkFeatureDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel.AttachStatus;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class AnnotationFeatureForm
    extends Form<AnnotatorState>
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationFeatureForm.class);
    
    public static final String ID_PREFIX = "featureEditorHead";
    
    private static final long serialVersionUID = 3635145598405490893L;

    // Top-level containers
    private final WebMarkupContainer noAnnotationWarning;
    private final WebMarkupContainer layerContainer;
    private final WebMarkupContainer buttonContainer;
    private final WebMarkupContainer infoContainer;
    private final WebMarkupContainer featureEditorContainer;

    // Within layerContainer
    private final WebMarkupContainer forwardAnnotationGroup;
    
    // Within infoContainer
    private final WebMarkupContainer selectedAnnotationInfoContainer;

    // Parent
    private final AnnotationDetailEditorPanel editorPanel;

    // Components
    private final Label selectedAnnotationLayer;
    private final CheckBox forwardAnnotationCheckBox;
    private final ConfirmationDialog deleteAnnotationDialog;
    private final ConfirmationDialog replaceAnnotationDialog;
    private final Label relationHint;
    private final DropDownChoice<AnnotationLayer> layerSelector;
    private final List<AnnotationLayer> annotationLayers = new ArrayList<>();
    private final FeatureEditorPanelContent featureEditorPanelContent;
    
    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserPreferencesService userPreferencesService;
    private @SpringBean UserDao userDao;

    AnnotationFeatureForm(AnnotationDetailEditorPanel aEditorPanel, String id,
        IModel<AnnotatorState> aState)
    {
        super(id, new CompoundPropertyModel<>(aState));
        setDefaultButton(null);
        editorPanel = aEditorPanel;

        add(deleteAnnotationDialog = createDeleteDialog());
        add(replaceAnnotationDialog = createReplaceDialog());
        
        noAnnotationWarning = new WebMarkupContainer("noAnnotationWarning");
        noAnnotationWarning.setOutputMarkupPlaceholderTag(true);
        noAnnotationWarning
                .add(visibleWhen(() -> !getModelObject().getSelection().getAnnotation().isSet()));
        add(noAnnotationWarning);

        // Trying to re-render the forwardAnnotationCheckBox as part of an AJAX request when it is
        // not visible causes an error in the JS console saying that the component could not be
        // found. Placing it in this WebMarkupContainer and controlling the visibility via the
        // container resolves this issue. Mind, using a "wicket:enclosure" instead of the
        // WebMarkupGroup also did not work.
        forwardAnnotationGroup = new WebMarkupContainer("forwardAnnotationGroup");
        forwardAnnotationGroup.add(forwardAnnotationCheckBox = createForwardAnnotationCheckBox());
        forwardAnnotationGroup.setOutputMarkupPlaceholderTag(true);
        forwardAnnotationGroup.add(visibleWhen(this::isForwardable));

        layerContainer = new WebMarkupContainer("layerContainer");
        layerContainer.setOutputMarkupPlaceholderTag(true);
        layerContainer.add(relationHint = createRelationHint());
        layerContainer.add(layerSelector = createDefaultAnnotationLayerSelector());
        // Visible if there is more than one selectable layer and if either "remember layer" is off
        // (meaning that the dropdown indicates the currently selected layer) or "remember layer"
        // is on and the document is editable (meaning we need to be able to change the layer)
        layerContainer.add(visibleWhen(() -> layerSelector.getChoicesModel()
                .map(layerChoices -> layerChoices.size() > 1).orElse(false).getObject()
                && (!getModelObject().getPreferences().isRememberLayer()
                        || editorPanel.getEditorPage().isEditable())));
        layerContainer.add(forwardAnnotationGroup);
        add(layerContainer);

        infoContainer = new WebMarkupContainer("infoContainer");
        infoContainer.setOutputMarkupPlaceholderTag(true);
        add(infoContainer);
        
        selectedAnnotationInfoContainer = new WebMarkupContainer("selectedAnnotationInfoContainer");
        selectedAnnotationInfoContainer.setOutputMarkupPlaceholderTag(true);
        selectedAnnotationInfoContainer.add(
                visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()));
        selectedAnnotationInfoContainer.add(createSelectedAnnotationTypeLabel());
        selectedAnnotationInfoContainer.add(createNoFeaturesWarningLabel());
        selectedAnnotationInfoContainer.add(createSelectedTextLabel());
        selectedAnnotationInfoContainer
                .add(selectedAnnotationLayer = createSelectedAnnotationLayerLabel());
        infoContainer.add(selectedAnnotationInfoContainer);

        featureEditorContainer = new WebMarkupContainer("featureEditorContainer");
        featureEditorContainer.setOutputMarkupPlaceholderTag(true);
        featureEditorContainer.add(featureEditorPanelContent = createFeatureEditorPanelContent());
        featureEditorContainer.add(createFocusResetHelper());
        featureEditorContainer.add(
                visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()));
        add(featureEditorContainer);
        
        buttonContainer = new WebMarkupContainer("buttonContainer");
        buttonContainer.setOutputMarkupPlaceholderTag(true);
        buttonContainer.add(createDeleteButton());
        buttonContainer.add(createReverseButton());
        buttonContainer.add(createClearButton());
        add(buttonContainer);
    }

    private TextField<String> createFocusResetHelper()
    {
        TextField<String> textfield = new TextField<>("focusResetHelper");
        textfield.setModel(Model.of());
        textfield.setOutputMarkupId(true);
        textfield.add(new AjaxPreventSubmitBehavior());
        textfield.add(new AjaxFormComponentUpdatingBehavior("focus")
        {
            private static final long serialVersionUID = -3030093250599939537L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                List<FeatureEditor> editors = new ArrayList<>();
                featureEditorPanelContent.getItems().next()
                        .visitChildren(FeatureEditor.class, (e, visit) -> {
                            editors.add((FeatureEditor) e);
                            visit.dontGoDeeper();
                        });
                
                if (!editors.isEmpty()) {
                    aTarget.focusComponent(editors.get(editors.size() - 1).getFocusComponent());
                }
            }
        });
        
        return textfield;
    }

    private Label createNoFeaturesWarningLabel()
    {
        Label label = new Label("noFeaturesWarning", "No features available!");
        label.add(visibleWhen(() -> getModelObject().getFeatureStates().isEmpty()));
        return label;
    }

    private FeatureEditorPanelContent createFeatureEditorPanelContent()
    {
        return new FeatureEditorPanelContent("featureEditors");
    }
    
    public Optional<FeatureEditor> getFirstFeatureEditor()
    {
        Iterator<Item<FeatureState>> itemIterator = featureEditorPanelContent.getItems();
        if (!itemIterator.hasNext()) {
            return Optional.empty();
        }
        else {
            return Optional.ofNullable((FeatureEditor) itemIterator.next().get("editor"));
        }
    }

    private ConfirmationDialog createDeleteDialog()
    {
        return new ConfirmationDialog("deleteAnnotationDialog",
                new StringResourceModel("DeleteDialog.title", this, null));
    }

    private ConfirmationDialog createReplaceDialog()
    {
        return new ConfirmationDialog("replaceAnnotationDialog",
                new StringResourceModel("ReplaceDialog.title", this, null),
                new StringResourceModel("ReplaceDialog.text", this, null));
    }

    private Label createSelectedAnnotationLayerLabel()
    {
        Label label = new Label("selectedAnnotationLayer", new Model<String>());
        label.setOutputMarkupPlaceholderTag(true);
        label.add(visibleWhen(() -> getModelObject().getPreferences().isRememberLayer()));
        return label;
    }

    private Label createSelectedAnnotationTypeLabel()
    {
        Label label = new Label("selectedAnnotationType", LoadableDetachableModel.of(() -> {
            try {
                return String.valueOf(selectFsByAddr(editorPanel.getEditorCas(),
                        getModelObject().getSelection().getAnnotation().getId())).trim();
            }
            catch (IOException e) {
                return "";
            }
        }));
        label.setOutputMarkupPlaceholderTag(true);
        // We show the extended info on the selected annotation only when run in development mode
        label.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && DEVELOPMENT.equals(getApplication().getConfigurationType())));
        return label;
    }

    private Label createRelationHint()
    {
        Label label = new Label("relationHint", Model.of());
        label.setOutputMarkupPlaceholderTag(true);
        label.setEscapeModelStrings(false);
        label.add(LambdaBehavior.onConfigure(_this -> {
            if (layerSelector.getModelObject() != null) {
                List<AnnotationLayer> relLayers = annotationService
                        .listAttachedRelationLayers(layerSelector.getModelObject());
                if (relLayers.isEmpty()) {
                    _this.setVisible(false);
                }
                else if (relLayers.size() == 1) {
                    _this.setDefaultModelObject("Create a <b>"
                            + escapeMarkup(relLayers.get(0).getUiName(), false, false)
                            + "</b> relation by drawing an arc between annotations of this layer.");
                    _this.setVisible(true);
                }
                else {
                    _this.setDefaultModelObject(
                            "Whoops! Found more than one relation layer attaching to this span layer!");
                    _this.setVisible(true);
                }
            }
            else {
                _this.setVisible(false);
            }
        }));
        return label;
    }
    
    private DropDownChoice<AnnotationLayer> createDefaultAnnotationLayerSelector()
    {
        DropDownChoice<AnnotationLayer> selector = new BootstrapSelect<>("defaultAnnotationLayer");
        selector.setChoices(new PropertyModel<>(this, "annotationLayers"));
        selector.setChoiceRenderer(new ChoiceRenderer<>("uiName"));
        selector.setOutputMarkupId(true);
        selector.add(LambdaAjaxFormComponentUpdatingBehavior.onUpdate("change",
                this::actionChangeDefaultLayer));
        return selector;
    }
    
    private void actionChangeDefaultLayer(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();

        aTarget.add(relationHint);
        aTarget.add(forwardAnnotationGroup);
        
        // If forward annotation was enabled, disable it
        if (state.isForwardAnnotation()) {
            state.setForwardAnnotation(false);
        }
        
        // If "remember layer" is set, the we really just update the selected layer...
        // we do not touch the selected annotation not the annotation detail panel
        if (!state.getPreferences().isRememberLayer()) {

            // If "remember layer" is not set, then changing the layer means that we
            // want to change the type of the currently selected annotation
            if (!Objects
                    .equals(state.getSelectedAnnotationLayer(), state.getDefaultAnnotationLayer())
                    && state.getSelection().getAnnotation().isSet()) {
                try {
                    if (state.getSelection().isArc()) {
                        editorPanel.actionClear(aTarget);
                    }
                    else {
                        actionReplace(aTarget);
                    }
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }
            // If no annotation is selected, then prime the annotation detail panel for the new type
            else {
                state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
                selectedAnnotationLayer.setDefaultModelObject(
                        Optional.ofNullable(state.getDefaultAnnotationLayer())
                                .map(AnnotationLayer::getUiName).orElse(null));
                aTarget.add(selectedAnnotationLayer);
                editorPanel.clearFeatureEditorModels(aTarget);
            }
        }
        
        // Save the currently selected layer as a user preference so it is remains active when a
        // user leaves the application and later comes back to continue annotating
        long prevDefaultLayer = state.getPreferences().getDefaultLayer();
        if (state.getDefaultAnnotationLayer() != null) {
            state.getPreferences().setDefaultLayer(state.getDefaultAnnotationLayer().getId());
        }
        else {
            state.getPreferences().setDefaultLayer(-1);
        }
        if (prevDefaultLayer != state.getPreferences().getDefaultLayer()) {
            try {
                userPreferencesService.savePreferences(state.getProject(),
                        userDao.getCurrentUser().getUsername(), state.getMode(),
                        state.getPreferences());
            }
            catch (IOException e) {
                handleException(this, aTarget, e);
            }
        }
    }

    private LambdaAjaxLink createSelectedTextLabel()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("jumpToAnnotation",
                this::actionJumpToAnnotation);
        link.add(new Label("selectedText", PropertyModel.of(getModelObject(),
                "selection.text")).setOutputMarkupId(true));
        link.setOutputMarkupId(true);
        return link;
    }
    
    private void actionJumpToAnnotation(AjaxRequestTarget aTarget) throws IOException
    {
        AnnotatorState state = getModelObject();
        
        editorPanel.getEditorPage().actionShowSelectedDocument(aTarget, state.getDocument(),
                state.getSelection().getBegin(), state.getSelection().getEnd());
    }

    private LambdaAjaxLink createClearButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("clear", editorPanel::actionClear);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && editorPanel.getEditorPage().isEditable()));
        return link;
    }

    private Component createReverseButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("reverse", editorPanel::actionReverse);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(LambdaBehavior.onConfigure(_this -> {
            AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
            
            _this.setVisible(state.getSelection().getAnnotation().isSet() 
                    && state.getSelection().isArc()
                    && RELATION_TYPE.equals(state.getSelectedAnnotationLayer().getType())
                    && editorPanel.getEditorPage().isEditable());

            // Avoid reversing in read-only layers
            _this.setEnabled(state.getSelectedAnnotationLayer() != null
                    && !state.getSelectedAnnotationLayer().isReadonly());
        }));
        return link;
    }

    private LambdaAjaxLink createDeleteButton()
    {
        LambdaAjaxLink link = new LambdaAjaxLink("delete", this::actionDelete);
        link.setOutputMarkupPlaceholderTag(true);
        link.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && editorPanel.getEditorPage().isEditable()));
        // Avoid deleting in read-only layers
        link.add(enabledWhen(() -> getModelObject().getSelectedAnnotationLayer() != null
                && !getModelObject().getSelectedAnnotationLayer().isReadonly()));
        return link;
    }
    
    private void actionDelete(AjaxRequestTarget aTarget) throws IOException, AnnotationException
    {
        AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
        
        AnnotationLayer layer = state.getSelectedAnnotationLayer();
        TypeAdapter adapter = annotationService.getAdapter(layer);

        CAS cas = editorPanel.getEditorCas();
        AnnotationFS fs = selectAnnotationByAddr(cas,
                state.getSelection().getAnnotation().getId());
        
        if (layer.isReadonly()) {
            error("Cannot replace an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        AttachStatus attachStatus = editorPanel.checkAttachStatus(aTarget, state.getProject(), fs);
        if (attachStatus.readOnlyAttached) {
            error("Cannot delete an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }        
        
        if (adapter instanceof SpanAdapter && attachStatus.attachCount > 0) {
            deleteAnnotationDialog.setContentModel(
                    new StringResourceModel("DeleteDialog.text", this, Model.of(layer))
                            .setParameters(attachStatus.attachCount));
            deleteAnnotationDialog.setConfirmAction((aCallbackTarget) -> {
                editorPanel.actionDelete(aCallbackTarget);
            });
            deleteAnnotationDialog.show(aTarget);
        }
        else {
            editorPanel.actionDelete(aTarget);
        }
    }
    
    private void actionReplace(AjaxRequestTarget aTarget) throws IOException
    {
        AnnotatorState state = AnnotationFeatureForm.this.getModelObject();

        AnnotationLayer newLayer = layerSelector.getModelObject();

        CAS cas = editorPanel.getEditorCas();
        AnnotationFS fs = selectAnnotationByAddr(cas,
                state.getSelection().getAnnotation().getId());
        AnnotationLayer currentLayer = annotationService.findLayer(state.getProject(), fs);
        
        if (currentLayer.isReadonly()) {
            error("Cannot replace an annotation on a read-only layer.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        AttachStatus attachStatus = editorPanel.checkAttachStatus(aTarget, state.getProject(), fs);
        if (attachStatus.readOnlyAttached) {
            error("Cannot replace an annotation to which annotations on read-only layers attach.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        
        replaceAnnotationDialog.setContentModel(
                new StringResourceModel("ReplaceDialog.text", AnnotationFeatureForm.this)
                        .setParameters(currentLayer.getUiName(), newLayer.getUiName(),
                                attachStatus.attachCount));
        replaceAnnotationDialog.setConfirmAction((_target) -> {
            // The delete action clears the selection, but we need it to create
            // the new annotation - so we save it.
            Selection savedSel = editorPanel.getModelObject().getSelection().copy();

            // Delete current annotation
            editorPanel.actionDelete(_target);

            // Set up the action to create the replacement annotation
            AnnotationLayer layer = layerSelector.getModelObject();
            state.getSelection().set(savedSel);
            state.getSelection().setAnnotation(VID.NONE_ID);
            state.setSelectedAnnotationLayer(layer);
            state.setDefaultAnnotationLayer(layer);
            selectedAnnotationLayer.setDefaultModelObject(layer.getUiName());
            editorPanel.loadFeatureEditorModels(_target);

            // Create the replacement annotation
            editorPanel.actionCreateOrUpdate(_target, editorPanel.getEditorCas());
            layerSelector.modelChanged();
            _target.add(AnnotationFeatureForm.this);
        });
        replaceAnnotationDialog.setCancelAction((_target) -> {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            _target.add(AnnotationFeatureForm.this);
        });
        replaceAnnotationDialog.show(aTarget);
    }

    /**
     * Part of <i>forward annotation</i> mode: creates the checkbox to toggle forward annotation
     * mode.
     */
    private CheckBox createForwardAnnotationCheckBox()
    {
        CheckBox checkbox = new CheckBox("forwardAnnotation");
        checkbox.setOutputMarkupId(true);
        checkbox.add(LambdaBehavior.onConfigure(_this -> {
            // Force-disable forward annotation mode if current layer is not forwardable
            if (!isForwardable()) {
                AnnotationFeatureForm.this.getModelObject().setForwardAnnotation(false);
            }
        }));
        checkbox.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> 
                focusForwardAnnotationComponent(_target, true)));
        
        return checkbox;
    }
    
    /**
     * Part of <i>forward annotation</i> mode: move focus to the hidden forward annotation input
     * field or to the free text component at the end of the rendering process
     * 
     * @param aResetSelectedTag
     *            whether to clear {@code selectedTag} if the forward features has a tagset. Has
     *            no effect if the forward feature is a free text feature.
     */
    private void focusForwardAnnotationComponent(AjaxRequestTarget aTarget,
            boolean aResetSelectedTag)
    {
        AnnotatorState state = getModelObject();
        if (!state.isForwardAnnotation()) {
            return;
        }
        
        List<AnnotationFeature> features = getEnabledAndVisibleFeatures(
                AnnotationFeatureForm.this.getModelObject()
                        .getDefaultAnnotationLayer());
        if (features.size() != 1) {
            // should not come here in the first place (controlled during
            // forward annotation process)
            return;
        }
        
        AnnotationFeature feature = features.get(0);
        
        // Check if this is a free text annotation or a tagset is attached. Use the hidden
        // forwardAnnotationText element only for tagset based forward annotations
        if (feature.getTagset() == null) {
            getFirstFeatureEditor()
                    .ifPresent(_editor -> autoFocus(aTarget, _editor.getFocusComponent()));
        }
        else {
            aTarget.focusComponent(editorPanel.getForwardAnnotationTextField());
            if (aResetSelectedTag) {
                editorPanel.setForwardAnnotationKeySequence("", "resetting on forward");
            }
        }
    }

    /**
     * Returns all enabled and visible features of the given annotation layer.
     */
    private List<AnnotationFeature> getEnabledAndVisibleFeatures(AnnotationLayer aLayer)
    {
        return annotationService.listAnnotationFeature(aLayer).stream()
                .filter(f -> f.isEnabled())
                .filter(f -> f.isVisible())
                .collect(Collectors.toList());
    }

    /**
     * Part of <i>forward annotation</i> mode: determines whether the currently selected layer is
     * forwardable or not.
     */
    private boolean isForwardable()
    {
        AnnotatorState state = getModelObject();
        
        // Fetch the current default layer (the one which determines the type of new annotations)
        AnnotationLayer layer = state.getDefaultAnnotationLayer();

        if (isNull(layer) || isNull(layer.getId())) {
            return false;
        }

        if (!SPAN_TYPE.equals(layer.getType())) {
            return false;
        }

        if (!SINGLE_TOKEN.equals(layer.getAnchoringMode())) {
            return false;
        }

        // Forward annotation mode requires that there is exactly one feature.
        // No forward annotation for multi-feature and zero-feature layers (where features count
        // which are are both enabled and visible).
        List<AnnotationFeature> features = getEnabledAndVisibleFeatures(layer);
        if (features.size() != 1) {
            return false;
        }

        AnnotationFeature feature = features.get(0);
        
        // Forward mode is only valid for string features
        if (!CAS.TYPE_NAME_STRING.equals(feature.getType())) {
            return false;
        }
        
        // If there is a tagset, it must have tags
        if (feature.getTagset() != null) {
            return !annotationService.listTags(feature.getTagset()).isEmpty();
        }
        else {
            return true;
        }
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        
        // set read only if annotation is finished or the user is viewing other's work
        setEnabled(editorPanel.getEditorPage().isEditable());
    }

    public void updateLayersDropdown()
    {
        AnnotatorState state = getModelObject();
        annotationLayers.clear();
        AnnotationLayer l = null;
        for (AnnotationLayer layer : state.getAnnotationLayers()) {
            if (!layer.isEnabled() || layer.isReadonly()
                || layer.getName().equals(Token.class.getName())) {
                continue;
            }
            if (layer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
                annotationLayers.add(layer);
                l = layer;
            }
            // manage chain type
            else if (layer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                for (AnnotationFeature feature : annotationService.listAnnotationFeature(layer)) {
                    if (!feature.isEnabled()) {
                        continue;
                    }
                    if (feature.getName().equals(WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
                        annotationLayers.add(layer);
                    }
                }
            }
            // chain
        }
        if (state.getDefaultAnnotationLayer() != null) {
            state.setSelectedAnnotationLayer(state.getDefaultAnnotationLayer());
        }
        else if (l != null) {
            state.setSelectedAnnotationLayer(l);
        }
    }

    void updateRememberLayer()
    {
        AnnotatorState state = getModelObject();
        if (state.getPreferences().isRememberLayer()) {
            if (state.getDefaultAnnotationLayer() == null) {
                state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
            }
        }
        else if (!state.getSelection().isArc()) {
            state.setDefaultAnnotationLayer(state.getSelectedAnnotationLayer());
        }

        // if no layer is selected in Settings
        if (state.getSelectedAnnotationLayer() != null) {
            selectedAnnotationLayer.setDefaultModelObject(
                state.getSelectedAnnotationLayer().getUiName());
        }
    }

    private class FeatureEditorPanelContent
        extends RefreshingView<FeatureState>
    {
        private static final long serialVersionUID = -8359786805333207043L;

        FeatureEditorPanelContent(String aId)
        {
            super(aId);
            setOutputMarkupId(true);
            // This strategy caches items as long as the panel exists. This is important to
            // allow the Kendo ComboBox datasources to be re-read when constraints change the
            // available tags.
            setItemReuseStrategy(new CachingReuseStrategy());
        }
        
        @Override
        protected void onAfterRender()
        {
            super.onAfterRender();
            
            RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(_target -> {
                // Put focus on hidden input field if we are in forward-mode unless the user has
                // selected an annotation which is not on the forward-mode layer
                AnnotatorState state = getModelObject();
                AnnotationLayer layer = state.getSelectedAnnotationLayer();
                if (
                        getModelObject().isForwardAnnotation() &&
                        layer != null &&
                        layer.equals(state.getDefaultAnnotationLayer())
                ) {
                    focusForwardAnnotationComponent(_target, false);
                }
                // If the user selects or creates an annotation then we put the focus on the
                // first of the feature editors
                else if (!Objects.equals(getRequestCycle().getMetaData(IsSidebarAction.INSTANCE),
                        true)) {
                    getFirstFeatureEditor().ifPresent(_editor -> 
                            autoFocus(_target, _editor.getFocusComponent()));
                }
            });
        }

        @Override
        protected void populateItem(final Item<FeatureState> item)
        {
            LOG.trace("FeatureEditorPanelContent.populateItem("
                + item.getModelObject().feature.getUiName() + ": "
                + item.getModelObject().value + ")");

            // Feature editors that allow multiple values may want to update themselves,
            // e.g. to add another slot.
            item.setOutputMarkupId(true);

            final FeatureState featureState = item.getModelObject();
            final FeatureEditor editor;
            
            // Look up a suitable editor and instantiate it
            FeatureSupport featureSupport = featureSupportRegistry
                    .getFeatureSupport(featureState.feature);
            editor = featureSupport.createEditor("editor", featureEditorContainer, editorPanel,
                    AnnotationFeatureForm.this.getModel(), item.getModel());

            // We need to enable the markup ID here because we use it during the AJAX behavior
            // that automatically saves feature editors on change/blur. 
            // Check addAnnotateActionBehavior.
            editor.setOutputMarkupId(true);
            editor.setOutputMarkupPlaceholderTag(true);
            
            // Ensure that markup IDs of feature editor focus components remain constant across
            // refreshes of the feature editor panel. This is required to restore the focus.
            editor.getFocusComponent().setOutputMarkupId(true);
            editor.getFocusComponent()
                    .setMarkupId(ID_PREFIX + editor.getModelObject().feature.getId());
            
            if (!featureState.feature.getLayer().isReadonly()) {
                AnnotatorState state = getModelObject();

                // Whenever it is updating an annotation, it updates automatically when a
                // component for the feature lost focus - but updating is for every component
                // edited LinkFeatureEditors must be excluded because the auto-update will break
                // the ability to add slots. Adding a slot is NOT an annotation action.
                if (state.getSelection().getAnnotation().isSet()
                    && !(editor instanceof LinkFeatureEditor)) {
                    addAnnotateActionBehavior(editor);
                }
                else if (!(editor instanceof LinkFeatureEditor)) {
                    addRefreshFeaturePanelBehavior(editor);
                }

                // Add tooltip on label
                StringBuilder tooltipTitle = new StringBuilder();
                tooltipTitle.append(featureState.feature.getUiName());
                if (featureState.feature.getTagset() != null) {
                    tooltipTitle.append(" (");
                    tooltipTitle.append(featureState.feature.getTagset().getName());
                    tooltipTitle.append(')');
                }

                Component labelComponent = editor.getLabelComponent();
//                labelComponent.setMarkupId(
//                        ID_PREFIX + editor.getModelObject().feature.getId() + "-w-lbl");
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                    featureState.feature.getDescription()));
            }
            else {
                editor.getFocusComponent().setEnabled(false);
            }
            
            item.add(editor);
        }

        private void addRefreshFeaturePanelBehavior(final FeatureEditor aFrag)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    AnnotationFeatureForm.this.refresh(aTarget);
                }
            });
        }

        private void addAnnotateActionBehavior(final FeatureEditor aFrag)
        {
            aFrag.addFeatureUpdateBehavior();
        }
        
        @Override
        protected Iterator<IModel<FeatureState>> getItemModels()
        {
            List<FeatureState> featureStates = getModelObject().getFeatureStates();

            return new ModelIteratorAdapter<FeatureState>(
                featureStates)
            {
                @Override
                protected IModel<FeatureState> model(FeatureState aObject)
                {
                    return FeatureStateModel.of(getModel(), aObject);
                }
            };
        }
    }

    private void actionFeatureUpdate(Component aComponent, AjaxRequestTarget aTarget)
    {
        try {
            AnnotatorState state = getModelObject();

            if (state.getConstraints() != null) {
                // Make sure we update the feature editor panel because due to
                // constraints the contents may have to be re-rendered
                AnnotationFeatureForm.this.refresh(aTarget);
            }
            
            // When updating an annotation in the sidebar, we must not force a
            // re-focus after rendering
            getRequestCycle().setMetaData(IsSidebarAction.INSTANCE, true);
            
            CAS cas = editorPanel.getEditorCas();
            AnnotationLayer layer = state.getSelectedAnnotationLayer();
            if (
                    state.isForwardAnnotation() &&
                    layer != null &&
                    layer.equals(state.getDefaultAnnotationLayer())
            ) {
                editorPanel.actionCreateForward(aTarget, cas);
            } else {
                editorPanel.actionCreateOrUpdate(aTarget, cas);
            }
            
            // If the focus was lost during the update, then try force-focusing the
            // next editor or the first one if we are on the last one.
            if (aTarget.getLastFocusedElementId() == null) {
                List<FeatureEditor> allEditors = new ArrayList<>();
                featureEditorPanelContent.visitChildren(FeatureEditor.class,
                    (editor, visit) -> {
                        allEditors.add((FeatureEditor) editor);
                        visit.dontGoDeeper();
                    });

                if (!allEditors.isEmpty()) {
                    FeatureEditor currentEditor = aComponent instanceof FeatureEditor
                            ? (FeatureEditor) aComponent
                            : aComponent.findParent(FeatureEditor.class);
                    
                    int i = allEditors.indexOf(currentEditor);
                    
                    // If the current editor cannot be found then move the focus to the
                    // first editor
                    if (i == -1) {
                        autoFocus(aTarget, allEditors.get(0).getFocusComponent());
                    }
                    // ... if it is the last one, say at the last one
                    else if (i >= (allEditors.size() - 1)) {
                        autoFocus(aTarget, allEditors.get(allEditors.size() - 1)
                                .getFocusComponent());
                    }
                    // ... otherwise move the focus to the next editor
                    else {
                        autoFocus(aTarget, allEditors.get(i + 1).getFocusComponent());
                    }
                }
            }
        }
        catch (Exception e) {
            handleException(this, aTarget, e);
        }
    }

    protected List<AnnotationLayer> getAnnotationLayers()
    {
        return annotationLayers;
    }

    public void refresh(AjaxRequestTarget aTarget)
    {
        aTarget.add(layerContainer, buttonContainer, infoContainer, featureEditorContainer,
                noAnnotationWarning);
    }
    
    protected DropDownChoice<AnnotationLayer> getLayerSelector()
    {
        return layerSelector;
    }
    
    private static final class IsSidebarAction extends MetaDataKey<Boolean> {
        private static final long serialVersionUID = 1L;
        
        public final static IsSidebarAction INSTANCE = new IsSidebarAction();
    }
    
    public void autoFocus(AjaxRequestTarget aTarget, Component aComponent)
    {
        // Check if any of the features suppresses auto-focus...
        for (FeatureState fstate : getModelObject().getFeatureStates()) {
            AnnotationFeature feature = fstate.getFeature();
            FeatureSupport<?> fs = featureSupportRegistry.getFeatureSupport(feature);
            if (fs.suppressAutoFocus(feature)) {
                return;
            }
        }
        
        aTarget.focusComponent(aComponent);
    }
    
    @OnEvent(stop = true)
    public void onFeatureUpdatedEvent(FeatureEditorValueChangedEvent aEvent)
    {
        AjaxRequestTarget target = aEvent.getTarget();
        actionFeatureUpdate(aEvent.getEditor(), target);
    }
    
    @OnEvent(stop = true)
    public void onLinkFeatureDeletedEvent(LinkFeatureDeletedEvent aEvent)
    {
        AjaxRequestTarget target = aEvent.getTarget();
        // Auto-commit if working on existing annotation
        if (getModelObject().getSelection().getAnnotation().isSet()) {
            try {
                editorPanel.actionCreateOrUpdate(target, editorPanel.getEditorCas());
            }
            catch (Exception e) {
                handleException(this, target, e);
            }
        }
    }
}
