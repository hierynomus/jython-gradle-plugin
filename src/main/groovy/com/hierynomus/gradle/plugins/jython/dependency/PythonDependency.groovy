/*
 * Copyright (C)2015 - Jeroen van Erp <jeroen@hierynomus.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hierynomus.gradle.plugins.jython.dependency

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency
import org.gradle.util.ConfigureUtil

class PythonDependency extends AbstractExternalModuleDependency {
    String moduleName
    List<CopiedArtifact> toCopy = []
    boolean useModuleName = true

    PythonDependency(String group, String name, String version, String configuration) {
        super(group, name, version, configuration)
        this.moduleName = name
    }

    static PythonDependency create(depInfo) {
        def pd
        if (depInfo instanceof String) {
            def split = depInfo.split(":")
            pd = new PythonDependency(split[0], split[1], split[2], null)
            if (split.length > 3) {
                pd.moduleName = split[3]
            }
        } else if (depInfo instanceof Map) {
            pd = new PythonDependency(depInfo.getOrDefault("group", null) as String, depInfo.get("name") as String, depInfo.get("version") as String, null)
            if (depInfo.containsKey('classifier')) {
                pd.moduleName = depInfo['classifier']
            }
        } else if (depInfo instanceof ExternalModuleDependency) {
            pd = new PythonDependency(depInfo.group, depInfo.name, depInfo.version, depInfo.configuration)
            if (depInfo.artifacts.size() == 1 && depInfo.artifacts[0].classifier) {
                pd.moduleName = depInfo.artifacts[0].classifier
            } else {
                pd.artifacts = depInfo.artifacts
            }
        } else {
            throw new IllegalArgumentException("Cannot convert $depInfo to PythonDependency")
        }
        return pd
    }

    static PythonDependency create(depInfo, closure) {
        PythonDependency dep = create(depInfo)
        ConfigureUtil.configure(closure, dep)
        return dep
    }

    @Override
    boolean contentEquals(Dependency dependency) {
        if(this == dependency) {
            return true;
        } else if(dependency != null && this.getClass() == dependency.getClass()) {
            ExternalModuleDependency that = (ExternalModuleDependency)dependency;
            return this.isContentEqualsFor(that);
        } else {
            return false;
        }
    }

    PythonDependency copy(Closure c) {
        def ca = new CopiedArtifact()
        ConfigureUtil.configure(c, ca)
        this.toCopy.add(ca)
        this
    }

    @Override
    ExternalModuleDependency copy() {
        PythonDependency copiedModuleDependency = new PythonDependency(this.getGroup(), this.getName(), this.getVersion(), this.getConfiguration())
        this.copyTo(copiedModuleDependency)
        return copiedModuleDependency
    }

    @Override
    String toString() {
        return "$group:$name:$version"
    }

    void setUseModuleName(boolean useModuleName) {
        this.useModuleName = useModuleName
    }

    void setModuleName(String moduleName) {
        this.moduleName = moduleName
    }

    static class CopiedArtifact {
        String from
        String into

        def from(String s) {
            this.from = s
        }

        def into(String s) {
            this.into = s
        }

        String getFrom() {
            return from
        }

        void setFrom(String from) {
            this.from = from
        }

        String getInto() {
            return into
        }

        void setInto(String into) {
            this.into = into
        }
    }
}
