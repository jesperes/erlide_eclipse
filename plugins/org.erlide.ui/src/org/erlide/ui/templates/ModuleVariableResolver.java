/*******************************************************************************
 * Copyright (c) 2004 Lukas Larsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lukas Larsson
 *******************************************************************************/

package org.erlide.ui.templates;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class ModuleVariableResolver extends TemplateVariableResolver {

    private static final List<ModuleVariableResolver> fInstances = new ArrayList<>();

    private String fModule = "<module_name>";

    public ModuleVariableResolver() {
        super("module", "The current erlang module");
        ModuleVariableResolver.fInstances.add(this);
    }

    public static ModuleVariableResolver getDefault() {
        if (ModuleVariableResolver.fInstances.isEmpty()) {
            ModuleVariableResolver.fInstances.add(new ModuleVariableResolver());
        }
        return ModuleVariableResolver.fInstances.get(0);
    }

    @Override
    public void resolve(final TemplateVariable variable,
            final TemplateContext theContext) {
        variable.setValue(fModule);
    }

    /**
     * @return Returns the module.
     */
    public String getModule() {
        return fModule;
    }

    /**
     * @param module
     *            The module to set.
     */
    public void setModule(final String module) {
        for (final Object element0 : ModuleVariableResolver.fInstances) {
            final ModuleVariableResolver element = (ModuleVariableResolver) element0;
            element.doSetModule(module);
        }
        doSetModule(module);
    }

    private void doSetModule(final String module) {
        fModule = module;
    }

}
