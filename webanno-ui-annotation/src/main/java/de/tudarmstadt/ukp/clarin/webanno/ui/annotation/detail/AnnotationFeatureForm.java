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

import com.googlecode.wicket.kendo.ui.form.TextField;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.SpanAnnotationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.util.JavascriptUtils;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultFocusBehavior2;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component
    .DeleteOrReplaceAnnotationModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.editor.*;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.util.ModelIteratorAdapter;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.time.Duration;

import java.util.*;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil.getAdapter;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail.AnnotationDetailEditorPanel
    .handleException;

public class AnnotationFeatureForm
    extends Form<AnnotatorState>
{
    private static final long serialVersionUID = 3635145598405490893L;

    // Add "featureEditorPanel" to AjaxRequestTargets instead of "featureEditorPanelContent"
    private WebMarkupContainer featureEditorPanel;
    private FeatureEditorPanelContent featureEditorPanelContent;

    private CheckBox forwardAnnotationCheck;
    private AjaxButton deleteButton;
    private AjaxButton reverseButton;
    private LayerSelector layerSelector;
    private String selectedTag = "";
    private Label selectedAnnotationLayer;
    private TextField<String> forwardAnnotationText;
    private ModalWindow deleteModal;
    private Label selectedTextLabel;
    private List<AnnotationLayer> annotationLayers = new ArrayList<>();

    private final AnnotationDetailEditorPanel editorPanel;

    void setSelectedTag(String selectedTag)
    {
        this.selectedTag = selectedTag;
    }

    TextField<String> getForwardAnnotationText()
    {
        return forwardAnnotationText;
    }

    Label getSelectedAnnotationLayer()
    {
        return selectedAnnotationLayer;
    }

    List<AnnotationLayer> getAnnotationLayers()
    {
        return annotationLayers;
    }

    WebMarkupContainer getFeatureEditorPanel()
    {
        return featureEditorPanel;
    }

    AnnotationFeatureForm(AnnotationDetailEditorPanel editorPanel, String id,
        IModel<AnnotatorState> aBModel)
    {
        super(id, new CompoundPropertyModel<>(aBModel));
        this.editorPanel = editorPanel;

        add(forwardAnnotationCheck = new CheckBox("forwardAnnotation")
        {
            private static final long serialVersionUID = 8908304272310098353L;

            {
                setOutputMarkupId(true);
                add(new AjaxFormComponentUpdatingBehavior("change")
                {
                    private static final long serialVersionUID = 5179816588460867471L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget aTarget)
                    {
                        updateForwardAnnotation();
                        if (AnnotationFeatureForm.this.getModelObject().isForwardAnnotation()) {
                            aTarget.appendJavaScript(JavascriptUtils.getFocusScript
                                (forwardAnnotationText));
                            selectedTag = "";
                        }
                    }
                });
            }

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setEnabled(editorPanel.isForwardable());
                updateForwardAnnotation();
            }
        });

        add(new Label("noAnnotationWarning", "No annotation selected!")
        {
            private static final long serialVersionUID = -6046409838139863541L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(!getModelObject().getSelection().getAnnotation().isSet());
            }
        });

        add(deleteButton = new AjaxButton("delete")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                setVisible(state.getSelection().getAnnotation().isSet());

                // Avoid deleting in read-only layers
                setEnabled(state.getSelectedAnnotationLayer() != null
                    && !state.getSelectedAnnotationLayer().isReadonly());
            }

            @Override
            public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                try {
                    AnnotatorState state = AnnotationFeatureForm.this.getModelObject();
                    JCas jCas = editorPanel.getEditorCas();
                    AnnotationFS fs = selectByAddr(jCas, state.getSelection().getAnnotation()
                        .getId());

                    AnnotationLayer layer = state.getSelectedAnnotationLayer();
                    TypeAdapter adapter = getAdapter(editorPanel.getAnnotationService(), layer);
                    if (adapter instanceof SpanAdapter && editorPanel.getAttachedRels(jCas, fs,
                        layer).size() > 0) {
                        deleteModal.setTitle("Are you sure you like to delete all attached " +
                            "relations to this span annotation?");
                        deleteModal.setContent(new DeleteOrReplaceAnnotationModalPanel(
                            deleteModal.getContentId(), state, deleteModal, editorPanel,
                            state.getSelectedAnnotationLayer(), false));
                        deleteModal.show(aTarget);
                    }
                    else {
                        editorPanel.actionDelete(aTarget);
                    }
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }
        });

        add(reverseButton = new AjaxButton("reverse")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                AnnotatorState state = AnnotationFeatureForm.this.getModelObject();

                setVisible(state.getSelection().getAnnotation().isSet()
                    && state.getSelection().isArc() && state.getSelectedAnnotationLayer().getType()
                    .equals(WebAnnoConst.RELATION_TYPE));

                // Avoid reversing in read-only layers
                setEnabled(state.getSelectedAnnotationLayer() != null
                    && !state.getSelectedAnnotationLayer().isReadonly());
            }

            @Override
            public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                try {
                    editorPanel.actionReverse(aTarget);
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }
        });
        reverseButton.setOutputMarkupPlaceholderTag(true);

        add(new AjaxButton("clear")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(AnnotationFeatureForm.this.getModelObject().getSelection()
                    .getAnnotation().isSet());
            }

            @Override
            public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
            {
                aTarget.addChildren(getPage(), FeedbackPanel.class);
                try {
                    editorPanel.actionClear(aTarget);
                }
                catch (Exception e) {
                    handleException(this, aTarget, e);
                }
            }
        });

        add(layerSelector = new AnnotationFeatureForm.LayerSelector
            ("defaultAnnotationLayer", annotationLayers));

        featureEditorPanel = new WebMarkupContainer("featureEditorsContainer")
        {
            private static final long serialVersionUID = 8908304272310098353L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getSelection().getAnnotation().isSet());
            }
        };
        // Add placeholder since wmc might start out invisible. Without the placeholder we
        // cannot make it visible in an AJAX call
        featureEditorPanel.setOutputMarkupPlaceholderTag(true);
        featureEditorPanel.setOutputMarkupId(true);

        featureEditorPanel.add(new Label("noFeaturesWarning", "No features available!")
        {
            private static final long serialVersionUID = 4398704672665066763L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getFeatureStates().isEmpty());
            }
        });

        featureEditorPanelContent = new AnnotationFeatureForm.FeatureEditorPanelContent
            ("featureValues");
        featureEditorPanel.add(featureEditorPanelContent);

        forwardAnnotationText = new TextField<>("forwardAnno");
        forwardAnnotationText.setOutputMarkupId(true);
        forwardAnnotationText.add(new AjaxFormComponentUpdatingBehavior("keyup")
        {
            private static final long serialVersionUID = 4554834769861958396L;

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
            {
                super.updateAjaxAttributes(attributes);

                IAjaxCallListener listener = new AjaxCallListener()
                {
                    private static final long serialVersionUID = -7968540662654079601L;

                    @Override
                    public CharSequence getPrecondition(Component component)
                    {
                        return "var keycode = Wicket.Event.keyCode(attrs.event);    return true;";
                    }
                };
                attributes.getAjaxCallListeners().add(listener);

                attributes.getDynamicExtraParameters().add("var eventKeycode = Wicket.Event" +
                    ".keyCode(attrs.event);return {keycode: eventKeycode};");
                attributes.setAllowDefault(true);
            }

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                final Request request = RequestCycle.get().getRequest();
                final String jsKeycode = request.getRequestParameters()
                    .getParameterValue("keycode").toString("");
                if (jsKeycode.equals("32")) {
                    try {
                        JCas jCas = editorPanel.getEditorCas();
                        editorPanel.actionCreateOrUpdate(aTarget, jCas);
                        selectedTag = "";
                    }
                    catch (Exception e) {
                        handleException(forwardAnnotationText, aTarget, e);
                    }
                    return;
                }
                if (jsKeycode.equals("13")) {
                    selectedTag = "";
                    return;
                }
                selectedTag = (forwardAnnotationText.getModelObject() == null ? ""
                    : forwardAnnotationText.getModelObject().charAt(0)) + selectedTag;
                Map<String, String> bindTags = getBindTags();
                if (!bindTags.isEmpty()) {
                    List<FeatureState> featureStates = getModelObject().getFeatureStates();
                    featureStates.get(0).value = getKeyBindValue(selectedTag, bindTags);
                }
                aTarget.add(forwardAnnotationText);
                aTarget.add(featureEditorPanelContent.get(0));
            }
        });
        forwardAnnotationText.setOutputMarkupId(true);
        forwardAnnotationText.add(new AttributeAppender("style", "opacity:0", ";"));
        // forwardAnno.add(new AttributeAppender("style", "filter:alpha(opacity=0)", ";"));
        add(forwardAnnotationText);

        // the selected text for annotation
        selectedTextLabel = new Label("selectedText", PropertyModel.of(getModelObject(),
            "selection.text"));
        selectedTextLabel.setOutputMarkupId(true);
        featureEditorPanel.add(selectedTextLabel);

        featureEditorPanel.add(new Label("layerName", "Layer")
        {
            private static final long serialVersionUID = 6084341323607243784L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getPreferences().isRememberLayer());
            }

        });
        featureEditorPanel.setOutputMarkupId(true);

        // the annotation layer for the selected annotation
        selectedAnnotationLayer = new Label("selectedAnnotationLayer", new Model<String>())
        {
            private static final long serialVersionUID = 4059460390544343324L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                setVisible(getModelObject().getPreferences().isRememberLayer());
            }

        };
        selectedAnnotationLayer.setOutputMarkupId(true);
        featureEditorPanel.add(selectedAnnotationLayer);
        add(featureEditorPanel);

        add(deleteModal = new ModalWindow("yesNoModal"));
        deleteModal.setOutputMarkupId(true);
        deleteModal.setInitialWidth(600);
        deleteModal.setInitialHeight(50);
        deleteModal.setResizable(true);
        deleteModal.setWidthUnit("px");
        deleteModal.setHeightUnit("px");
        deleteModal.setTitle("Are you sure you want to delete the existing annotation?");
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        // Avoid reversing in read-only layers
        setEnabled(getModelObject().getDocument() != null && !editorPanel.isAnnotationFinished());
    }

    private String getKeyBindValue(String aKey, Map<String, String> aBindTags)
    {
        // check if all the key pressed are the same character
        // if not, just check a Tag for the last char pressed
        if (aKey.isEmpty()) {
            return aBindTags.get(aBindTags.keySet().iterator().next());
        }
        char prevC = aKey.charAt(0);
        for (char ch : aKey.toCharArray()) {
            if (ch != prevC) {
                break;
            }
        }

        if (aBindTags.get(aKey) != null) {
            return aBindTags.get(aKey);
        }
        // re-cycle suggestions
        if (aBindTags.containsKey(aKey.substring(0, 1))) {
            selectedTag = aKey.substring(0, 1);
            return aBindTags.get(aKey.substring(0, 1));
        }
        // set it to the first in the tag list , when arbitrary key is pressed
        return aBindTags.get(aBindTags.keySet().iterator().next());
    }

    Map<String, String> getBindTags()
    {
        AnnotationFeature f = editorPanel.getAnnotationService()
            .listAnnotationFeature(getModelObject().getSelectedAnnotationLayer()).get(0);
        TagSet tagSet = f.getTagset();
        Map<Character, String> tagNames = new LinkedHashMap<>();
        Map<String, String> bindTag2Key = new LinkedHashMap<>();
        for (Tag tag : editorPanel.getAnnotationService().listTags(tagSet)) {
            if (tagNames.containsKey(tag.getName().toLowerCase().charAt(0))) {
                String oldBinding = tagNames.get(tag.getName().toLowerCase().charAt(0));
                String newBinding = oldBinding + tag.getName().toLowerCase().charAt(0);
                tagNames.put(tag.getName().toLowerCase().charAt(0), newBinding);
                bindTag2Key.put(newBinding, tag.getName());
            }
            else {
                tagNames.put(tag.getName().toLowerCase().charAt(0),
                    tag.getName().toLowerCase().substring(0, 1));
                bindTag2Key.put(tag.getName().toLowerCase().substring(0, 1), tag.getName());
            }
        }
        return bindTag2Key;

    }

    private void updateForwardAnnotation()
    {
        AnnotatorState state = getModelObject();
        if (state.getSelectedAnnotationLayer() != null
            && !state.getSelectedAnnotationLayer().isLockToTokenOffset()) {
            state.setForwardAnnotation(false);// no forwarding for
            // sub-/multi-token annotation
        }
        else {
            state.setForwardAnnotation(state.isForwardAnnotation());
        }
    }

    void updateLayersDropdown()
    {
        editorPanel.getLog().trace("updateLayersDropdown()");

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
                for (AnnotationFeature feature : editorPanel.getAnnotationService()
                    .listAnnotationFeature(layer)) {
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
        editorPanel.getLog().trace("updateRememberLayer()");

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

    public class LayerSelector
        extends DropDownChoice<AnnotationLayer>
    {
        private static final long serialVersionUID = 2233133653137312264L;

        LayerSelector(String aId, List<? extends AnnotationLayer> aChoices)
        {
            super(aId, aChoices);
            setOutputMarkupId(true);
            setChoiceRenderer(new ChoiceRenderer<>("uiName"));
            add(new AjaxFormComponentUpdatingBehavior("change")
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    AnnotatorState state = AnnotationFeatureForm.this.getModelObject();

                    // If "remember layer" is set, the we really just update the selected
                    // layer...
                    // we do not touch the selected annotation not the annotation detail panel
                    if (state.getPreferences().isRememberLayer()) {
                        state.setSelectedAnnotationLayer(getModelObject());
                    }
                    // If "remember layer" is not set, then changing the layer means that we
                    // want to change the type of the currently selected annotation
                    else if (!state.getSelectedAnnotationLayer().equals(getModelObject())
                        && state.getSelection().getAnnotation().isSet()) {
                        if (state.getSelection().isArc()) {
                            try {
                                editorPanel.actionClear(aTarget);
                            }
                            catch (Exception e) {
                                handleException(AnnotationFeatureForm.LayerSelector.this,
                                    aTarget, e);
                            }
                        }
                        else {
                            deleteModal.setContent(new DeleteOrReplaceAnnotationModalPanel
                                (deleteModal.getContentId(), state, deleteModal, editorPanel,
                                    getModelObject(), true));

                            deleteModal.setWindowClosedCallback(new ModalWindow
                                .WindowClosedCallback()
                            {
                                private static final long serialVersionUID =
                                    4364820331676014559L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                    target.add(AnnotationFeatureForm.this);
                                }
                            });
                            deleteModal.show(aTarget);
                        }
                    }
                    // If no annotation is selected, then prime the annotation detail panel for
                    // the new type
                    else {
                        state.setSelectedAnnotationLayer(getModelObject());
                        selectedAnnotationLayer.setDefaultModelObject(getModelObject().getUiName());
                        aTarget.add(selectedAnnotationLayer);
                        editorPanel.clearFeatureEditorModels(aTarget);
                    }
                }
            });
        }
    }

    public class FeatureEditorPanelContent
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

        @SuppressWarnings("rawtypes")
        @Override
        protected void populateItem(final Item<FeatureState> item)
        {
            editorPanel.getLog().trace("FeatureEditorPanelContent.populateItem("
                + item.getModelObject().feature.getUiName() + ": "
                + item.getModelObject().value + ")");

            // Feature editors that allow multiple values may want to update themselves,
            // e.g. to add another slot.
            item.setOutputMarkupId(true);

            final FeatureState featureState = item.getModelObject();
            final FeatureEditor frag;

            switch (featureState.feature.getMultiValueMode()) {
                case NONE: {
                    switch (featureState.feature.getType()) {
                        case CAS.TYPE_NAME_INTEGER: {
                            frag = new NumberFeatureEditor("editor", "numberFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                            break;
                        }
                        case CAS.TYPE_NAME_FLOAT: {
                            frag = new NumberFeatureEditor("editor", "numberFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                            break;
                        }
                        case CAS.TYPE_NAME_BOOLEAN: {
                            frag = new BooleanFeatureEditor("editor", "booleanFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                            break;
                        }
                        case CAS.TYPE_NAME_STRING: {
                            frag = new TextFeatureEditor("editor", "textFeatureEditor",
                                AnnotationFeatureForm.this, item.getModel());
                            break;
                        }
                        default:
                            throw new IllegalArgumentException(
                                "Unsupported type [" + featureState.feature.getType()
                                    + "] on feature [" + featureState.feature.getName() + "]");
                    }
                    break;
                }
                case ARRAY: {
                    switch (featureState.feature.getLinkMode()) {
                        case WITH_ROLE: {
                            // If it is none of the primitive types, it must be a link feature
                            frag = new LinkFeatureEditor("editor", "linkFeatureEditor",
                                editorPanel, item.getModel());
                            break;
                        }
                        default:
                            throw new IllegalArgumentException(
                                "Unsupported link mode [" + featureState.feature.getLinkMode()
                                    + "] on feature [" + featureState.feature.getName() + "]");
                    }
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported multi-value mode ["
                        + featureState.feature.getMultiValueMode() + "] on feature ["
                        + featureState.feature.getName() + "]");
            }

            if (!featureState.feature.getLayer().isReadonly()) {
                AnnotatorState state = getModelObject();

                // Whenever it is updating an annotation, it updates automatically when a
                // component for the feature lost focus - but updating is for every component
                // edited LinkFeatureEditors must be excluded because the auto-update will break
                // the ability to add slots. Adding a slot is NOT an annotation action.
                if (state.getSelection().getAnnotation().isSet()
                    && !(frag instanceof LinkFeatureEditor)) {
                    addAnnotateActionBehavior(frag, "change");
                }
                else if (!(frag instanceof LinkFeatureEditor)) {
                    addRefreshFeaturePanelBehavior(frag, "change");
                }

                // Put focus on hidden input field if we are in forward-mode
                if (state.isForwardAnnotation()) {
                    forwardAnnotationText.add(new DefaultFocusBehavior2());
                }
                // Put focus on first component if we select an existing annotation or create a
                // new one
                else if (item.getIndex() == 0
                    && SpanAnnotationResponse.is(state.getAction().getUserAction())) {
                    frag.getFocusComponent().add(new DefaultFocusBehavior());
                }
                // Restore/preserve focus when tabbing through the feature editors
                else if (state.getAction().getUserAction() == null) {
                    AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
                    if (target != null && frag.getFocusComponent().getMarkupId()
                        .equals(target.getLastFocusedElementId())) {
                        target.focusComponent(frag.getFocusComponent());
                    }
                }

                // Add tooltip on label
                StringBuilder tooltipTitle = new StringBuilder();
                tooltipTitle.append(featureState.feature.getUiName());
                if (featureState.feature.getTagset() != null) {
                    tooltipTitle.append(" (");
                    tooltipTitle.append(featureState.feature.getTagset().getName());
                    tooltipTitle.append(')');
                }

                Component labelComponent = frag.getLabelComponent();
                labelComponent.add(new AttributeAppender("style", "cursor: help", ";"));
                labelComponent.add(new DescriptionTooltipBehavior(tooltipTitle.toString(),
                    featureState.feature.getDescription()));
            }
            else {
                frag.getFocusComponent().setEnabled(false);
            }

            // We need to enable the markup ID here because we use it during the AJAX behavior
            // that
            // automatically saves feature editors on change/blur. Check
            // addAnnotateActionBehavior.
            frag.setOutputMarkupId(true);
            item.add(frag);
        }

        private void addRefreshFeaturePanelBehavior(final FeatureEditor aFrag, String aEvent)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    aTarget.add(featureEditorPanel);
                }
            });
        }

        private void addAnnotateActionBehavior(final FeatureEditor aFrag, String aEvent)
        {
            aFrag.getFocusComponent().add(new AjaxFormComponentUpdatingBehavior(aEvent)
            {
                private static final long serialVersionUID = 5179816588460867471L;

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes aAttributes)
                {
                    super.updateAjaxAttributes(aAttributes);
                    // When focus is on a feature editor and the user selects a new annotation,
                    // there is a race condition between the saving the value of the feature
                    // editor and the loading of the new annotation. Delay the feature editor
                    // save to give preference to loading the new annotation.
                    aAttributes.setThrottlingSettings(new ThrottlingSettings(getMarkupId(),
                        Duration.milliseconds(250), true));
                    aAttributes.getAjaxCallListeners().add(new AjaxCallListener()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public CharSequence getPrecondition(Component aComponent)
                        {
                            // If the panel refreshes because the user selects a new annotation,
                            // the annotation editor panel is updated for the new annotation
                            // first (before saving values) because of the delay set above. When
                            // the delay is over, we can no longer save the value because the
                            // old component is no longer there. We use the markup id of the
                            // editor fragments to check if the old component is still there
                            // (i.e. if the user has just tabbed to a new field) or if the old
                            // component is gone (i.e. the user selected/created another
                            // annotation). If the old component is no longer there, we abort
                            // the delayed save action.
                            return "return $('#" + aFrag.getMarkupId() + "').length > 0;";
                        }
                    });
                }

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    try {
                        AnnotatorState state = getModelObject();
                        if (state.getConstraints() != null) {
                            // Make sure we update the feature editor panel because due to
                            // constraints the contents may have to be re-rendered
                            aTarget.add(featureEditorPanel);
                        }
                        JCas jCas = editorPanel.getEditorCas();
                        editorPanel.actionCreateOrUpdate(aTarget, jCas);
                    }
                    catch (Exception e) {
                        handleException(AnnotationFeatureForm.FeatureEditorPanelContent.this,
                            aTarget, e);
                    }
                }
            });
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
}