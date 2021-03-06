/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.python.packaging;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.packaging.ui.PyCondaManagementService;
import com.jetbrains.python.packaging.ui.PyPackageManagementService;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.sdk.PythonSdkType;
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author yole
 */
public class PyPackageManagersImpl extends PyPackageManagers {
  private final Map<String, PyPackageManagerImpl> myInstances = new HashMap<String, PyPackageManagerImpl>();

  @NotNull
  public synchronized PyPackageManager forSdk(@NotNull final Sdk sdk) {
    final String homePath = sdk.getHomePath();
    if (homePath == null) {
      return new DummyPackageManager(sdk);
    }
    PyPackageManagerImpl manager = myInstances.get(homePath);
    if (manager == null) {
      if (PythonSdkType.isRemote(sdk)) {
        manager = new PyRemotePackageManagerImpl(homePath);
      }
      else if (PyCondaPackageManagerImpl.isCondaVEnv(sdk) && PyCondaPackageService.getCondaExecutable(sdk.getHomeDirectory()) != null) {
        manager = new PyCondaPackageManagerImpl(homePath);
      }
      else {
        manager = new PyPackageManagerImpl(homePath);
      }
      myInstances.put(homePath, manager);
    }
    return manager;
  }

  public PyPackageManagementService getManagementService(Project project, Sdk sdk) {
    if (PyCondaPackageManagerImpl.isCondaVEnv(sdk)) {
      return new PyCondaManagementService(project, sdk);
    }
    return new PyPackageManagementService(project, sdk);
  }

  static class DummyPackageManager extends PyPackageManager {
    private final String myName;
    private final LanguageLevel myLanguageLevel;
    private final PythonSdkFlavor myFlavor;

    public DummyPackageManager(@NotNull final Sdk sdk) {
      myName = sdk.getName();
      myLanguageLevel = PythonSdkType.getLanguageLevelForSdk(sdk);
      myFlavor = PythonSdkFlavor.getFlavor(sdk);
    }

    @Override
    public void installManagement() throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @NotNull
    private String getErrorMessage() {
      return "Invalid interpreter \"" + myName + "\" version: " + myLanguageLevel.toString() + " type: " + myFlavor.getName();
    }

    @Override
    public boolean hasManagement(boolean cachedOnly) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @Override
    public void install(@NotNull String requirementString) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @Override
    public void install(@NotNull List<PyRequirement> requirements, @NotNull List<String> extraArgs) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @Override
    public void uninstall(@NotNull List<PyPackage> packages) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @Override
    public void refresh() {
    }

    @NotNull
    @Override
    public String createVirtualEnv(@NotNull String destinationDir, boolean useGlobalSite) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @Nullable
    @Override
    public List<PyPackage> getPackages(boolean cachedOnly) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @Nullable
    @Override
    public PyPackage findPackage(@NotNull String name, boolean cachedOnly) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }

    @Nullable
    @Override
    public List<PyRequirement> getRequirements(@NotNull Module module) {
      return null;
    }

    @Nullable
    @Override
    public Set<PyPackage> getDependents(@NotNull PyPackage pkg) throws ExecutionException {
      throw new ExecutionException(getErrorMessage());
    }
  }
  
}
