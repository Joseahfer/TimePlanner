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
 * imitations under the License.
 */
package ru.aleshin.features.home.impl.presentation.ui.templates.screenmodel

import kotlinx.coroutines.delay
import ru.aleshin.core.utils.extensions.duration
import ru.aleshin.core.utils.functional.Constants
import ru.aleshin.core.utils.functional.Either
import ru.aleshin.core.utils.platform.screenmodel.work.ActionResult
import ru.aleshin.core.utils.platform.screenmodel.work.EffectResult
import ru.aleshin.core.utils.platform.screenmodel.work.WorkCommand
import ru.aleshin.core.utils.platform.screenmodel.work.WorkProcessor
import ru.aleshin.core.utils.platform.screenmodel.work.WorkResult
import ru.aleshin.features.home.impl.domain.interactors.TemplatesInteractor
import ru.aleshin.features.home.impl.presentation.models.TemplatesSortedType
import ru.aleshin.features.home.impl.presentation.ui.templates.contract.TemplatesAction
import ru.aleshin.features.home.impl.presentation.ui.templates.contract.TemplatesEffect
import javax.inject.Inject

/**
 * @author Stanislav Aleshin on 08.05.2023.
 */
internal interface TemplatesWorkProcessor : WorkProcessor<TemplatesWorkCommand, TemplatesAction, TemplatesEffect> {

    class Base @Inject constructor(
        private val templatesInteractor: TemplatesInteractor,
    ) : TemplatesWorkProcessor {

        override suspend fun work(command: TemplatesWorkCommand) = when (command) {
            is TemplatesWorkCommand.LoadTemplates -> {
                delay(Constants.Delay.LOAD_ANIMATION)
                loadTemplatesWork(command.sortedType)
            }
            is TemplatesWorkCommand.DeleteTemplate -> deleteTemplateWork(command.id, command.sortedType)
        }

        private suspend fun loadTemplatesWork(
            sortedType: TemplatesSortedType,
        ): WorkResult<TemplatesAction, TemplatesEffect> {
            return when (val templates = templatesInteractor.fetchTemplates()) {
                is Either.Right -> {
                    val sortedTemplates = when (sortedType) {
                        TemplatesSortedType.DATE -> templates.data.sortedBy { it.startTime }
                        TemplatesSortedType.CATEGORIES -> templates.data.sortedBy { it.category.id }
                        TemplatesSortedType.DURATION -> templates.data.sortedBy { duration(it.startTime, it.endTime) }
                    }
                    ActionResult(TemplatesAction.UpdateTemplates(sortedTemplates))
                }
                is Either.Left -> EffectResult(TemplatesEffect.ShowError(templates.data))
            }
        }

        private suspend fun deleteTemplateWork(
            templateId: Int,
            sortedType: TemplatesSortedType,
        ): WorkResult<TemplatesAction, TemplatesEffect> {
            val deleteResult = templatesInteractor.deleteTemplate(templateId)
            return when (deleteResult) {
                is Either.Right -> loadTemplatesWork(sortedType)
                is Either.Left -> EffectResult(TemplatesEffect.ShowError(deleteResult.data))
            }
        }
    }
}

internal sealed class TemplatesWorkCommand : WorkCommand {
    data class LoadTemplates(val sortedType: TemplatesSortedType) : TemplatesWorkCommand()
    data class DeleteTemplate(val id: Int, val sortedType: TemplatesSortedType) : TemplatesWorkCommand()
}