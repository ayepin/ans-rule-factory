/**
 * Copyright © 2010-2014 Nokia
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo

import com.fasterxml.jackson.databind.JsonNode
import org.jsonschema2pojo.rules.RuleFactory
import org.jsonschema2pojo.util.URLUtil

import java.net.URI
import java.util.HashMap

class LocalRuleFactory : RuleFactory() {
    private val localContentResolver: LocalContentResolver
    private val urlToHandle: String

    init {
        val schemaStore = SchemaStore()
        localContentResolver = LocalContentResolver()
        schemaStore.contentResolver = localContentResolver
        setSchemaStore(schemaStore)
        //E.g. https://raw.githubusercontent.com/washingtonpost/ans-schema/master/src/main/resources/schema/ans/0.5.8/
        urlToHandle = System.getenv(URL_PROPERTY) ?: System.getProperty(URL_PROPERTY) ?: throw IllegalStateException("URL_PROPERTY is not specified")
    }

    override fun setGenerationConfig(config: GenerationConfig) {
        super.setGenerationConfig(config)

        val sources = config.source
        while (sources.hasNext()) {
            val source = sources.next()
            val dirString = source.toString()
            if (URLUtil.parseProtocol(dirString) == URLProtocol.FILE && URLUtil.getFileFromURL(source).isDirectory) {
                localContentResolver.registerLocalCopy(urlToHandle, dirString)
            }
        }
    }

    internal class LocalContentResolver : ContentResolver() {

        override fun resolve(uri: URI): JsonNode {
            var uri = uri
            val localCopy = localCopy(uri.toString())
            if (localCopy != null) {
                uri = URI.create(localCopy)
            }
            return super.resolve(uri)
        }

        fun registerLocalCopy(url: String, dir: String) {
            LOCAL_MAP.put(url, dir)
        }

        private fun localCopy(url: String): String? {
            for ((key, value) in LOCAL_MAP) {
                if (url.startsWith(key)) {
                    return url.replace(key, value)
                }
            }
            return null
        }

        companion object {

            private val LOCAL_MAP = HashMap<String, String>()
        }
    }

    companion object {
        private val URL_PROPERTY = "json_schema_url"
    }
}
