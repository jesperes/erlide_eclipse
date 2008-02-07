package org.erlide.core;

import org.eclipse.core.resources.IResource;

public interface IMarkerGenerator {

	void addMarker(IResource file, IResource compiledFile, String errorDesc,
			int lineNumber, int severity, String errorVar);
}
