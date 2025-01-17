/*
 * Copyright 2023 Stanislav Aleshin
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
package ru.aleshin.features.home.api.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.aleshin.features.home.api.domain.entities.template.Template

/**
 * @author Stanislav Aleshin on 08.03.2023.
 */
interface TemplatesRepository {
    suspend fun addTemplates(templates: List<Template>)
    suspend fun addTemplate(template: Template): Int
    suspend fun fetchTemplatesById(templateId: Int): Template?
    fun fetchAllTemplates(): Flow<List<Template>>
    suspend fun updateTemplate(template: Template)
    suspend fun deleteTemplateById(id: Int)
    suspend fun deleteAllTemplates()
}
