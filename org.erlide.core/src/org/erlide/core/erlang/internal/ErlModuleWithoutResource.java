package org.erlide.core.erlang.internal;

import org.erlide.core.erlang.ErlangCore;
import org.erlide.core.erlang.IErlElement;

public class ErlModuleWithoutResource extends ErlModule {
	private final String path;

	protected ErlModuleWithoutResource(final IErlElement parent,
			final String nameWithExt, final String initialText,
			final String path) {
		super(parent, nameWithExt, initialText, null);
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.erlide.core.erlang.internal.ErlModule#getFilePath(org.eclipse.core
	 * .resources.IResource)
	 */
	@Override
	protected String getFilePath() {
		return path;
	}

	@Override
	public void dispose() {
		super.dispose();
		ErlangCore.getModelManager().removeModule(this);
	}

	@Override
	protected boolean useCaches() {
		return false;
	}

}
