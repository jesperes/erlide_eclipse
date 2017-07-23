/*******************************************************************************
 * Copyright (c) 2004 Vlad Dumitrescu and others. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Eclipse Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Vlad Dumitrescu
 *******************************************************************************/

package org.erlide.ui.editors.erl.outline;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.erlide.core.ErlangCore;
import org.erlide.engine.ErlangEngine;
import org.erlide.engine.model.IErlElement;
import org.erlide.engine.model.erlang.ISourceReference;
import org.erlide.engine.model.root.IErlModelChangeListener;
import org.erlide.engine.model.root.IErlModule;
import org.erlide.ui.ErlideImage;
import org.erlide.ui.actions.ActionMessages;
import org.erlide.ui.actions.CompositeActionGroup;
import org.erlide.ui.actions.ErlangSearchActionGroup;
import org.erlide.ui.actions.SortAction;
import org.erlide.ui.editors.erl.ErlangEditor;
import org.erlide.ui.editors.erl.IErlangHelpContextIds;
import org.erlide.ui.editors.erl.outline.filters.FilterDescriptor;
import org.erlide.ui.editors.erl.outline.filters.OutlineFilterUtils;
import org.erlide.ui.editors.erl.outline.filters.PatternFilter;
import org.erlide.ui.internal.ErlideUIPlugin;
import org.erlide.ui.navigator.ErlElementSorter;
import org.erlide.ui.prefs.PreferenceConstants;
import org.erlide.ui.prefs.plugin.ErlEditorMessages;
import org.erlide.ui.util.ErlModelUtils;
import org.erlide.util.ErlLogger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 *
 * @author Vlad Dumitrescu
 *
 */

public class ErlangOutlinePage extends ContentOutlinePage
        implements IErlModelChangeListener, ISortableContentOutlinePage {

    IErlModule fModule;
    private ErlangEditor fEditor;
    private CompositeActionGroup fActionGroups;
    private TreeViewer fOutlineViewer;
    private SortAction fSortAction;
    private OpenAndLinkWithEditorHelper fOpenAndLinkWithEditorHelper;
    private ToggleLinkingAction fToggleLinkingAction;
    private final PatternFilter fPatternFilter = new PatternFilter();
    private IPartListener fPartListener;

    @Override
    public Control getControl() {
        return fOutlineViewer.getControl();
    }

    @Override
    public ISelection getSelection() {
        return fOutlineViewer.getSelection();
    }

    @Override
    public TreeViewer getTreeViewer() {
        return fOutlineViewer;
    }

    @Override
    public void setFocus() {
        getControl().setFocus();
    }

    @Override
    public void setSelection(final ISelection selection) {
        fOutlineViewer.setSelection(selection);
    }

    /**
     *
     * @param documentProvider
     *
     * @param editor
     *
     */
    public ErlangOutlinePage(final ErlangEditor editor) {
        // myDocProvider = documentProvider;
        fEditor = editor;
        ErlangEngine.getInstance().getModel().addModelChangeListener(this);
    }

    /**
     *
     * @param editorInput
     *
     */
    public void setInput(final IEditorInput editorInput) {
        fModule = null;
        try {
            fModule = ErlModelUtils.getModule(editorInput);
            if (fModule != null) {
                fModule.open(null);
            }
            // addFilters();
        } catch (final CoreException e) {
        }
    }

    private void addFilters() {
        final IEclipsePreferences prefsNode = ErlangOutlinePage.getPrefsNode();
        final Collection<FilterDescriptor> filterDescriptors = FilterDescriptor
                .getFilterDescriptors();
        final List<FilterDescriptor> descs = Lists.newArrayList();
        for (final FilterDescriptor filterDescriptor : filterDescriptors) {
            final String filterId = filterDescriptor.getId();
            final boolean value = prefsNode.getBoolean(filterId, false);
            if (value) {
                descs.add(filterDescriptor);
            }
        }
        OutlineFilterUtils.setFilters(descs, this);
        final List<String> userDefinedPatterns = Lists.newArrayList();
        final Set<String> enabledFilterIDs = Sets.newHashSet();
        final List<String> emptyList = Lists.newArrayList();
        final Set<String> emptySet = Sets.newHashSet();
        final boolean userFiltersEnabled = OutlineFilterUtils
                .loadViewDefaults(userDefinedPatterns, enabledFilterIDs);
        if (!userFiltersEnabled) {
            userDefinedPatterns.clear();
        }
        OutlineFilterUtils.updateViewerFilters(getTreeViewer(), emptyList, emptySet,
                userDefinedPatterns, enabledFilterIDs, getPatternFilter());
    }

    public void refresh() {
        if (getTreeViewer() != null) {
            final Control c = getTreeViewer().getControl();
            if (c.isDisposed()) {
                return;
            }
            final Display d = c.getDisplay();
            d.asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (getTreeViewer().getControl() != null
                            && !getTreeViewer().getControl().isDisposed()) {
                        getTreeViewer().refresh(fModule);
                    }
                }
            });
        }
    }

    @Override
    public void createControl(final Composite parent) {
        final Tree tree = new Tree(parent, SWT.MULTI);
        fOutlineViewer = new TreeViewer(tree);
        fOutlineViewer.setAutoExpandLevel(0);
        fOutlineViewer.setUseHashlookup(true);
        fOutlineViewer.setContentProvider(fEditor.createOutlineContentProvider());
        fOutlineViewer.setLabelProvider(fEditor.createOutlineLabelProvider());
        fOutlineViewer.addPostSelectionChangedListener(this);
        fOutlineViewer.setInput(fModule);
        final IPageSite site = getSite();
        fOpenAndLinkWithEditorHelper = new OpenAndLinkWithEditorHelper(fOutlineViewer,
                fEditor, site.getPage());

        final IContextService service = site.getService(IContextService.class);
        if (service != null) {
            service.activateContext("org.erlide.ui.erlangOutlineAndNavigatorScope");
        }

        final MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager m) {
                // recursive loop?
                // menuAboutToShow(m);
                contextMenuAboutToShow(m);
            }
        });
        final Menu menu = manager.createContextMenu(tree);
        tree.setMenu(menu);

        site.registerContextMenu(ErlangCore.PLUGIN_ID + ".outline", manager,
                fOutlineViewer);
        fActionGroups = new CompositeActionGroup(
                new ActionGroup[] { new ErlangSearchActionGroup(this) });
        // register global actions
        final IActionBars actionBars = site.getActionBars();
        actionBars.setGlobalActionHandler(ITextEditorActionConstants.UNDO,
                fEditor.getAction(ITextEditorActionConstants.UNDO));
        actionBars.setGlobalActionHandler(ITextEditorActionConstants.REDO,
                fEditor.getAction(ITextEditorActionConstants.REDO));
        fActionGroups.fillActionBars(actionBars);
        registerToolbarActions(actionBars);
        final IHandlerService handlerService = site.getService(IHandlerService.class);
        if (handlerService != null) {
            handlerService.activateHandler(
                    IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR,
                    new ActionHandler(fToggleLinkingAction));
        }
        fPartListener = new IPartListener() {

            @Override
            public void partOpened(final IWorkbenchPart part) {
                addFilters(); // JC borde filter-metoden ovan r�cka?
            }

            @Override
            public void partDeactivated(final IWorkbenchPart part) {
            }

            @Override
            public void partClosed(final IWorkbenchPart part) {
            }

            @Override
            public void partBroughtToTop(final IWorkbenchPart part) {
            }

            @Override
            public void partActivated(final IWorkbenchPart part) {
                addFilters();
            }
        };
        getSite().getPage().addPartListener(fPartListener);
    }

    protected void contextMenuAboutToShow(final IMenuManager menu) {
        ErlideUIPlugin.createStandardGroups(menu);
        final IStructuredSelection selection = (IStructuredSelection) getSelection();
        fActionGroups.setContext(new ActionContext(selection));
        fActionGroups.fillContextMenu(menu);
    }

    public static class NoModuleElement extends WorkbenchAdapter implements IAdaptable {

        /*
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return ErlEditorMessages.ErlangOutlinePage_error_noelement;
        }

        /*
         *
         * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
         */
        @Override
        public <T> T getAdapter(final Class<T> adapter) {
            if (adapter == IWorkbenchAdapter.class) {
                return adapter.cast(this);
            }
            return null;
        }
    }

    public void select(final ISourceReference reference) {
        if (getTreeViewer() != null) {
            ISelection s = getTreeViewer().getSelection();
            if (s instanceof IStructuredSelection) {
                final IStructuredSelection ss = (IStructuredSelection) s;
                final List<?> elements = ss.toList();
                if (!elements.contains(reference)) {
                    s = reference == null ? StructuredSelection.EMPTY
                            : new StructuredSelection(reference);
                    getTreeViewer().setSelection(s, true);
                }
            }
        }
    }

    @Override
    public void dispose() {
        getSite().getPage().removePartListener(fPartListener);
        if (fEditor != null) {
            fEditor.outlinePageClosed();
            fEditor = null;
        }
        ErlangEngine.getInstance().getModel().removeModelChangeListener(this);

        super.dispose();
    }

    @Override
    public void elementChanged(final IErlElement element) {
        if (fModule == element) {
            refresh();
        }
    }

    /**
     * @param actionBars
     */
    private void registerToolbarActions(final IActionBars actionBars) {
        final IToolBarManager toolBarManager = actionBars.getToolBarManager();
        fSortAction = new SortAction(getTreeViewer(), "Sort",
                new ErlElementSorter(ErlElementSorter.SORT_ON_NAME),
                new ErlElementSorter(ErlElementSorter.SORT_ON_EXPORT), null, false,
                ErlideUIPlugin.getDefault().getPreferenceStore());
        toolBarManager.add(fSortAction);

        final IMenuManager viewMenuManager = actionBars.getMenuManager();
        fToggleLinkingAction = new ToggleLinkingAction();
        fToggleLinkingAction.setActionDefinitionId(
                IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR);
        viewMenuManager.add(fToggleLinkingAction);
    }

    @Override
    public void sort(final boolean sorting) {
        ErlLogger.debug("NYI: sorting " + sorting);
    }

    public static IEclipsePreferences getPrefsNode() {
        final String qualifier = ErlideUIPlugin.PLUGIN_ID;
        final IScopeContext context = InstanceScope.INSTANCE;
        final IEclipsePreferences eclipsePreferences = context.getNode(qualifier);
        return eclipsePreferences;
    }

    /**
     * This action toggles whether this Erlang Outline page links its selection to the
     * active editor.
     *
     * @since 3.0
     */
    public class ToggleLinkingAction extends Action {

        /**
         * Constructs a new action.
         */
        public ToggleLinkingAction() {
            super(ActionMessages.ToggleLinkingAction_label);
            setDescription(ActionMessages.ToggleLinkingAction_description);
            setToolTipText(ActionMessages.ToggleLinkingAction_tooltip);
            ErlideImage.setLocalImageDescriptors(this, "synced.gif");
            PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
                    IErlangHelpContextIds.LINK_EDITOR_ACTION);
            final IEclipsePreferences prefsNode = ErlangOutlinePage.getPrefsNode();
            final boolean isLinkingEnabled = prefsNode.getBoolean(
                    PreferenceConstants.ERLANG_OUTLINE_LINK_WITH_EDITOR, true);
            setChecked(isLinkingEnabled);
            fOpenAndLinkWithEditorHelper.setLinkWithEditor(isLinkingEnabled);
        }

        /**
         * Runs the action.
         */
        @Override
        public void run() {
            final IEclipsePreferences prefsNode = ErlangOutlinePage.getPrefsNode();
            final boolean isChecked = isChecked();
            prefsNode.putBoolean(PreferenceConstants.ERLANG_OUTLINE_LINK_WITH_EDITOR,
                    isChecked);
            if (isChecked && fEditor != null) {
                getTreeViewer().refresh();
            }
            fOpenAndLinkWithEditorHelper.setLinkWithEditor(isChecked);
        }

    }

    public PatternFilter getPatternFilter() {
        return fPatternFilter;
    }

    public boolean isLinkedWithEditor() {
        return fOpenAndLinkWithEditorHelper.isLinkedWithEditor();
    }

}
