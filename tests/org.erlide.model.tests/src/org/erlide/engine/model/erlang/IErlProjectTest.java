package org.erlide.engine.model.erlang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.erlide.engine.internal.model.root.ErlProject;
import org.erlide.engine.model.root.ErlangProjectProperties;
import org.erlide.engine.model.root.IErlModule;
import org.erlide.engine.model.root.IErlProject;
import org.erlide.engine.util.ErlideTestUtils;
import org.erlide.runtime.runtimeinfo.RuntimeInfo;
import org.erlide.runtime.runtimeinfo.RuntimeVersion;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class IErlProjectTest extends ErlModelTestBase {

    private static final String XX_ERLIDEX = "xx.erlidex";

    // Collection<IErlModule> getModules() throws ErlModelException;
    @Test
    public void getModules() throws Exception {
        ErlideTestUtils.createInclude(ErlModelTestBase.projects[0], "bb.erl", "-module(bb).\n");
        ErlideTestUtils.createModule(ErlModelTestBase.projects[0], "cc.hrl", "-define(A, hej).\n");
        ErlideTestUtils.createInclude(ErlModelTestBase.projects[0], "dd.hrl", "-define(B, du).\n");
        final List<IErlModule> expected = Lists.newArrayList(module);
        final Collection<IErlModule> modules = ErlModelTestBase.projects[0].getModules();
        assertEquals(expected, modules);
    }

    // FIXME write tests that gives exceptions!

    // Collection<IErlModule> getIncludes() throws ErlModelException;
    @Test
    public void getIncludes() throws Exception {
        ErlideTestUtils.createModule(ErlModelTestBase.projects[0], "aa.erl", "-module(aa).\n");
        ErlideTestUtils.createInclude(ErlModelTestBase.projects[0], "bb.erl", "-module(bb).\n");
        ErlideTestUtils.createModule(ErlModelTestBase.projects[0], "cc.hrl", "-define(A, hej).\n");
        final IErlModule includeDD = ErlideTestUtils.createInclude(ErlModelTestBase.projects[0], "dd.hrl",
                "-define(B, du).\n");
        final List<IErlModule> expected = Lists.newArrayList(includeDD);
        final Collection<IErlModule> includes = ErlModelTestBase.projects[0].getIncludes();
        assertEquals(expected, includes);
    }

    // Collection<IErlModule> getModulesAndIncludes() throws ErlModelException;
    @Test
    public void getModulesAndIncludes() throws Exception {
        ErlideTestUtils.createInclude(ErlModelTestBase.projects[0], "bb.erl", "-module(bb).\n");
        ErlideTestUtils.createModule(ErlModelTestBase.projects[0], "cc.hrl", "-define(A, hej).\n");
        final IErlModule includeD = ErlideTestUtils.createInclude(ErlModelTestBase.projects[0], "dd.hrl",
                "-define(B, du).\n");
        final List<IErlModule> expected = Lists.newArrayList(module, includeD);
        final Collection<IErlModule> includes = ErlModelTestBase.projects[0].getModulesAndIncludes();
        assertEquals(expected, includes);
    }

    // Collection<IErlModule> getExternalModules() throws ErlModelException;
    // void setExternalModulesFile(String absolutePath)
    // throws BackingStoreException;
    @Test
    public void getExternalModules() throws Exception {
        File externalFile = null;
        File externalsFile = null;
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final String externalModulesString = aProject.getProperties()
                .getExternalModules();
        try {
            // given
            // an erlang project and an external file not in any project
            final String externalFileName = "external6.erl";
            externalFile = ErlideTestUtils.createTmpFile(externalFileName,
                    "-module(external6).\nf([_ | _]=L ->\n    atom_to_list(L).\n");
            final String absolutePath = externalFile.getAbsolutePath();
            externalsFile = ErlideTestUtils.createTmpFile(IErlProjectTest.XX_ERLIDEX, absolutePath);
            aProject.open(null);
            final Collection<IErlModule> otpModules = aProject.getExternalModules();
            ((ErlProject) aProject)
                    .setExternalModulesFile(externalsFile.getAbsolutePath());
            aProject.open(null);
            // when
            // fetching all external modules
            final Collection<IErlModule> externalModules = aProject.getExternalModules();
            // then
            // the external file should be returned
            final Set<IErlModule> otpSet = Sets.newHashSet(otpModules);
            final Set<IErlModule> externalSet = Sets.newHashSet(externalModules);
            final Set<IErlModule> difference = Sets.difference(externalSet, otpSet);
            assertEquals(1, difference.size());
            final IErlModule externalModule = difference.iterator().next();
            assertNotNull(externalModule);
            assertEquals(absolutePath, externalModule.getFilePath());
        } finally {
            if (externalFile != null && externalFile.exists()) {
                externalFile.delete();
            }
            if (externalsFile != null && externalsFile.exists()) {
                externalsFile.delete();
            }
            ((ErlProject) aProject).setExternalModulesFile(externalModulesString);
        }
    }

    // Collection<IErlModule> getExternalIncludes() throws ErlModelException;
    // void setExternalIncludesFile(String absolutePath)
    // throws BackingStoreException;
    @Test
    public void getExternalIncludes() throws Exception {
        File externalFile = null;
        File externalsFile = null;
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final String externalIncludesString = aProject.getProperties()
                .getExternalIncludes();
        try {
            // given
            // an erlang project and an external file not in any project
            final String externalFileName = "external.hrl";
            externalFile = ErlideTestUtils.createTmpFile(externalFileName,
                    "-define(E, hej).\n");
            final String absolutePath = externalFile.getAbsolutePath();
            final String externalsFileName = IErlProjectTest.XX_ERLIDEX;
            externalsFile = ErlideTestUtils.createTmpFile(externalsFileName,
                    absolutePath);
            aProject.open(null);
            final Collection<IErlModule> otpIncludes = aProject.getExternalIncludes();
            ((ErlProject) aProject)
                    .setExternalIncludesFile(externalsFile.getAbsolutePath());
            aProject.open(null);
            // when
            // fetching all external includes
            final Collection<IErlModule> externalIncludes = aProject
                    .getExternalIncludes();
            // then
            // the external file should be returned
            final Set<IErlModule> otpSet = Sets.newHashSet(otpIncludes);
            final Set<IErlModule> externalSet = Sets.newHashSet(externalIncludes);
            final Set<IErlModule> difference = Sets.difference(externalSet, otpSet);
            assertEquals(1, difference.size());
            final IErlModule externalInclude = difference.iterator().next();
            assertNotNull(externalInclude);
            assertEquals(absolutePath, externalInclude.getFilePath());
        } finally {
            if (externalFile != null && externalFile.exists()) {
                externalFile.delete();
            }
            if (externalsFile != null && externalsFile.exists()) {
                externalsFile.delete();
            }
            ((ErlProject) aProject).setExternalIncludesFile(externalIncludesString);
        }
    }

    @Test
    public void getExternalIncludes_includeDirs() throws Exception {
        File externalFile = null;
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final Collection<IPath> includeDirs = aProject.getProperties().getIncludeDirs();
        try {
            // given
            // an erlang project and an external file not in any project, but on
            // the include-path
            final String externalFileName = "external.hrl";
            externalFile = ErlideTestUtils.createTmpFile(externalFileName,
                    "-define(E, hej).\n");
            final String absolutePath = externalFile.getAbsolutePath();
            final List<IPath> newIncludeDirs = Lists.newArrayList(includeDirs);
            aProject.open(null);
            final Collection<IErlModule> otpIncludes = aProject.getExternalIncludes();
            final IPath absoluteDir = new Path(absolutePath).removeLastSegments(1);
            newIncludeDirs.add(absoluteDir);
            ((ErlProject) aProject).setIncludeDirs(newIncludeDirs);
            aProject.open(null);
            // when
            // fetching all external includes
            final Collection<IErlModule> externalIncludes = aProject
                    .getExternalIncludes();
            // then
            // the external file should be returned
            final Set<IErlModule> otpSet = Sets.newHashSet(otpIncludes);
            final Set<IErlModule> externalSet = Sets.newHashSet(externalIncludes);
            final Set<IErlModule> difference = Sets.difference(externalSet, otpSet);
            final IErlModule externalInclude = difference.iterator().next();
            assertNotNull(externalInclude);
            assertEquals(absolutePath, externalInclude.getFilePath());
        } finally {
            if (externalFile != null && externalFile.exists()) {
                externalFile.delete();
            }
            ((ErlProject) aProject).setIncludeDirs(includeDirs);
        }
    }

    // String getExternalModulesString();
    @Test
    public void getExternalModulesString() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final String externalIncludesString = aProject.getProperties()
                .getExternalIncludes();
        try {
            final String s = "/hej";
            ((ErlProject) aProject).setExternalModulesFile(s);
            assertEquals(s, aProject.getProperties().getExternalModules());
        } finally {
            ((ErlProject) aProject).setExternalModulesFile(externalIncludesString);
        }
    }

    // String getExternalIncludesString();
    @Test
    public void getExternalIncludesString() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final String externalIncludesString = aProject.getProperties()
                .getExternalIncludes();
        try {
            final String s = "/tjo";
            ((ErlProject) aProject).setExternalIncludesFile(s);
            assertEquals(s, aProject.getProperties().getExternalIncludes());
        } finally {
            ((ErlProject) aProject).setExternalIncludesFile(externalIncludesString);
        }
    }

    // void setIncludeDirs(Collection<IPath> includeDirs)
    // throws BackingStoreException;
    @Test
    public void setIncludeDirs() throws Exception {
        File externalFile = null;
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final Collection<IPath> includeDirs = aProject.getProperties().getIncludeDirs();
        try {
            // given
            // an erlang project and an external file not in any project
            final String externalFileName = "external.hrl";
            externalFile = ErlideTestUtils.createTmpFile(externalFileName,
                    "-define(E, hej).\n");
            final String absolutePath = externalFile.getAbsolutePath();
            final List<IPath> newIncludeDirs = Lists.newArrayList(includeDirs);
            aProject.open(null);
            final Collection<IErlModule> otpIncludes = aProject.getExternalIncludes();
            final IPath absoluteDir = new Path(absolutePath).removeLastSegments(1);
            newIncludeDirs.add(absoluteDir);
            ((ErlProject) aProject).setIncludeDirs(newIncludeDirs);
            aProject.open(null);
            // when
            // fetching all external includes
            final Collection<IErlModule> externalIncludes = aProject
                    .getExternalIncludes();
            // then
            // the external file should be returned
            final Set<IErlModule> otpSet = Sets.newHashSet(otpIncludes);
            final Set<IErlModule> externalSet = Sets.newHashSet(externalIncludes);
            final Set<IErlModule> difference = Sets.difference(externalSet, otpSet);
            assertEquals(1, difference.size());
            final IErlModule externalInclude = difference.iterator().next();
            assertNotNull(externalInclude);
            assertEquals(new Path(absolutePath), new Path(externalInclude.getFilePath()));
        } finally {
            if (externalFile != null && externalFile.exists()) {
                externalFile.delete();
            }
            ((ErlProject) aProject).setIncludeDirs(includeDirs);
        }
    }

    // void setSourceDirs(Collection<IPath> sourceDirs)
    // throws BackingStoreException;
    @Test
    public void setSourceDirs() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final Collection<IPath> sourceDirs = aProject.getProperties().getSourceDirs();
        try {
            // given
            // an Erlang project and a module
            final IPath srcxPath = new Path("srcx");
            final List<IPath> srcxDirs = Lists.newArrayList(srcxPath);
            aProject.open(null);
            // when
            // setting source dirs so the module is on source path
            final Collection<IErlModule> modules = aProject.getModules();
            ((ErlProject) aProject).setSourceDirs(srcxDirs);
            aProject.open(null);
            final Collection<IErlModule> srcxModules = aProject.getModules();
            ((ErlProject) aProject).setSourceDirs(sourceDirs);
            aProject.open(null);
            final Collection<IErlModule> modulesAgain = aProject.getModules();
            // then
            // the it should be returned, but not otherwise
            assertEquals(0, srcxModules.size());
            assertEquals(1, modules.size());
            assertEquals(module, modules.iterator().next());
            assertEquals(1, modulesAgain.size());
            assertEquals(module, modulesAgain.iterator().next());
        } finally {
            ((ErlProject) aProject).setSourceDirs(sourceDirs);
        }
    }

    // Collection<IPath> getSourceDirs();
    @Test
    public void getSourceDirs() throws Exception {
        final Collection<IPath> sourceDirs = ErlModelTestBase.projects[0].getProperties().getSourceDirs();
        assertEquals(1, sourceDirs.size());
        final IPath path = new Path("src");
        assertEquals(path, sourceDirs.iterator().next());
    }

    // Collection<IPath> getIncludeDirs();
    @Test
    public void getIncludeDirs() throws Exception {
        final Collection<IPath> includeDirs = ErlModelTestBase.projects[0].getProperties()
                .getIncludeDirs();
        assertEquals(1, includeDirs.size());
        final IPath path = new Path("include");
        assertEquals(path, includeDirs.iterator().next());
    }

    // IPath getOutputLocation();
    @Test
    public void getOutputLocation() throws Exception {
        final IPath outputLocation = ErlModelTestBase.projects[0].getProperties().getOutputDir();
        assertEquals(new Path("ebin"), outputLocation);
    }

    // RuntimeInfo getRuntimeInfo();
    @Test
    public void getRuntimeInfo() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final RuntimeInfo info = aProject.getRuntimeInfo();
        // final String expected = ResourcesPlugin.getWorkspace().getRoot()
        // .getLocation().toString();
        assertNotNull(info);
        // The working dir might be relative to the project and can also be "."
        // We need to convert it to a canonical absolute path in order to be
        // able to compare it with a value.
        // This is not very portable across OSs
    }

    // RuntimeVersion getRuntimeVersion();
    @Test
    public void getRuntimeVersion() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final RuntimeVersion version = aProject.getRuntimeVersion();
        assertNotNull(version);
        final int majorVersion = version.getMajor();
        assertTrue(majorVersion >= 12);
    }

    // TODO check more properties than source dirs property
    @Test
    public void setProperties() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final Collection<IPath> sourceDirs = aProject.getProperties().getSourceDirs();
        try {
            final ErlangProjectProperties properties = aProject.getProperties();
            final IPath srcx = new Path("srcx");
            properties.setSourceDirs(Lists.newArrayList(srcx));
            aProject.setProperties(properties);
            final Collection<IPath> sourceDirs2 = aProject.getProperties()
                    .getSourceDirs();
            assertEquals(1, sourceDirs2.size());
            assertEquals(srcx, sourceDirs2.iterator().next());
        } finally {
            ((ErlProject) aProject).setSourceDirs(sourceDirs);
        }
    }

    @Test
    public void getReferencedProjects() throws Exception {
        final IProject aProject = ErlModelTestBase.projects[0].getWorkspaceProject();
        final IProjectDescription description = aProject.getDescription();
        final IProject[] refs = {ErlModelTestBase.projects[1].getWorkspaceProject() };
        try {
            description.setReferencedProjects(refs);
            aProject.setDescription(description, null);
            final List<IErlProject> expected = Lists.newArrayList(ErlModelTestBase.projects[1]);
            assertEquals(expected, ErlModelTestBase.projects[0].getReferencedProjects());
        } finally {
            description.setReferencedProjects(new IProject[0]);
            aProject.setDescription(description, null);
        }
    }

    public void getProjectReferences_closedProject() throws Exception {
        final IErlProject erlProject = ErlModelTestBase.projects[0];
        final IProject aProject = erlProject.getWorkspaceProject();
        try {
            aProject.close(null);
            erlProject.getReferencedProjects();
        } finally {
            if (!aProject.isOpen()) {
                aProject.open(null);
            }
        }
    }

    // IErlModule getModule(String name) throws ErlModelException;
    @Test
    public void getModule() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final Collection<IPath> sourceDirs = aProject.getProperties().getSourceDirs();
        try {
            // given
            // an Erlang project and a module
            final IErlModule aModule = ErlideTestUtils.createModule(aProject, "aa.erl",
                    "-module(aa).\n");
            final IPath srcxPath = new Path("srcx");
            final List<IPath> srcxDirs = Lists.newArrayList(srcxPath);
            aProject.open(null);
            // when
            // setting source dirs so the module is on source path
            final IErlModule module2 = aProject.getModule("aa");
            final IErlModule nullModule = aProject.getModule("aa.hrl");
            final IErlModule nullModule2 = aProject.getModule("AA");
            final IErlModule nullModule3 = aProject.getModule("aA");
            final IErlModule nullModule4 = aProject.getModule("AA.erl");
            final IErlModule module4 = aProject.getModule("aa.erl");
            ((ErlProject) aProject).setSourceDirs(srcxDirs);
            aProject.open(null);
            final IErlModule srcxModule = aProject.getModule("aa");
            ((ErlProject) aProject).setSourceDirs(sourceDirs);
            aProject.open(null);
            final IErlModule module3 = aProject.getModule("aa");
            // then
            // the it should be returned, but not otherwise
            assertEquals(aModule, module2);
            assertNull(srcxModule);
            assertNull(nullModule);
            assertNull(nullModule2);
            assertNull(nullModule3);
            assertNull(nullModule4);
            assertEquals(aModule, module3);
            assertEquals(aModule, module4);
        } finally {
            ((ErlProject) aProject).setSourceDirs(sourceDirs);
        }
    }

    // IProject getWorkspaceProject();
    @Test
    public void getWorkspaceProject() throws Exception {
        final IErlProject aProject = ErlModelTestBase.projects[0];
        final IProject workspaceProject = aProject.getWorkspaceProject();
        assertNotNull(workspaceProject);
        assertEquals(aProject.getName(), workspaceProject.getName());
    }
}
