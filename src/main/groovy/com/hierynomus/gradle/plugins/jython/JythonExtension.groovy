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
package com.hierynomus.gradle.plugins.jython

import com.hierynomus.gradle.plugins.jython.repository.PypiRepository
import com.hierynomus.gradle.plugins.jython.repository.Repository
import com.hierynomus.gradle.plugins.jython.repository.UrlRepository

class JythonExtension {

    File pyCacheDir

    def sourceRepositories = [
            new PypiRepository(),
            new UrlRepository('https://github.com/${dep.group}/${dep.name}/archive/${dep.version}.tar.gz')
    ]

    void setSourceRepositories(Collection sourceRepositories) {
        this.sourceRepositories = []
        sourceRepositories.forEach({ r ->
            if (r instanceof String && r == "pypi") {
                this.sourceRepositories.add(new PypiRepository())
            } else if (r instanceof String) {
                this.sourceRepositories.add(new UrlRepository(r))
            } else if (r instanceof Repository) {
                this.sourceRepositories.add(r)
            } else {
                throw new IllegalArgumentException("Don't know how to convert $r to Repository")
            }
        })
    }
}
