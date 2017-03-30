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

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.internal.artifacts.dependencies.AbstractExternalModuleDependency
import org.gradle.util.ConfigureUtil

class PythonDependency extends AbstractExternalModuleDependency {
    private CopySpec copySpec
    private Project project

    PythonDependency(String group, String name, String version, String configuration, Project project) {
        super(group, name, version, configuration)
        this.project = project
        this.copySpec = project.copySpec()
    }

    static PythonDependency create(depInfo, Project project) {
        if (depInfo instanceof String) {
            def split = depInfo.split(":")
            return new PythonDependency(split[0], split[1], split[2], null, project)
        } else if (depInfo instanceof Map) {
            return new PythonDependency(depInfo.getOrDefault("group", null) as String, depInfo.get("name") as String, depInfo.get("version") as String, null, project)
        } else if (depInfo instanceof ExternalModuleDependency) {
            return new PythonDependency(depInfo.group, depInfo.name, depInfo.version, depInfo.configuration, project)
        }
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

//    PythonDependency configure(Closure<?> closure) {
//        closure.setDelegate(this)
//        closure()
//        return this
//    }

    def copy(Closure<?> closure) {
        ConfigureUtil.configure(closure, copySpec)
    }

    @Override
    ExternalModuleDependency copy() {
        PythonDependency copiedModuleDependency = new PythonDependency(this.getGroup(), this.getName(), this.getVersion(), this.getConfiguration(), project);
        this.copyTo(copiedModuleDependency);
        return copiedModuleDependency;
    }
}
