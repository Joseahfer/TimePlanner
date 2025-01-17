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
package ru.aleshin.features.home.impl.presentation.ui.home.screenModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import ru.aleshin.core.utils.functional.Constants
import ru.aleshin.core.utils.functional.Either
import ru.aleshin.core.utils.functional.handle
import ru.aleshin.core.utils.functional.rightOrElse
import ru.aleshin.core.utils.managers.DateManager
import ru.aleshin.core.utils.platform.screenmodel.work.*
import ru.aleshin.features.editor.api.presentation.TimeTaskAlarmManager
import ru.aleshin.features.home.api.domain.entities.schedules.TimeTask
import ru.aleshin.features.home.api.domain.entities.schedules.TimeTaskStatus
import ru.aleshin.features.home.impl.domain.common.convertToTimeTask
import ru.aleshin.features.home.impl.domain.interactors.ScheduleInteractor
import ru.aleshin.features.home.impl.domain.interactors.TimeShiftInteractor
import ru.aleshin.features.home.impl.presentation.common.TimeTaskStatusController
import ru.aleshin.features.home.impl.presentation.mapppers.schedules.ScheduleDomainToUiMapper
import ru.aleshin.features.home.impl.presentation.mapppers.schedules.mapToDomain
import ru.aleshin.features.home.impl.presentation.mapppers.templates.mapToDomain
import ru.aleshin.features.home.impl.presentation.mapppers.templates.mapToUi
import ru.aleshin.features.home.impl.presentation.models.schedules.ScheduleUi
import ru.aleshin.features.home.impl.presentation.models.schedules.TimeTaskUi
import ru.aleshin.features.home.impl.presentation.models.templates.TemplateUi
import ru.aleshin.features.home.impl.presentation.ui.home.contract.HomeAction
import ru.aleshin.features.home.impl.presentation.ui.home.contract.HomeEffect
import java.util.*
import javax.inject.Inject

/**
 * @author Stanislav Aleshin on 25.02.2023.
 */
internal interface ScheduleWorkProcessor : FlowWorkProcessor<ScheduleWorkCommand, HomeAction, HomeEffect> {

    class Base @Inject constructor(
        private val scheduleInteractor: ScheduleInteractor,
        private val timeShiftInteractor: TimeShiftInteractor,
        private val mapperToUi: ScheduleDomainToUiMapper,
        private val statusController: TimeTaskStatusController,
        private val timeTaskAlarmManager: TimeTaskAlarmManager,
        private val dateManager: DateManager,
    ) : ScheduleWorkProcessor {

        override suspend fun work(command: ScheduleWorkCommand) = when (command) {
            is ScheduleWorkCommand.LoadScheduleByDate -> loadScheduleByDateWork(command.date)
            is ScheduleWorkCommand.ChangeTaskDoneState -> changeTaskDoneStateWork(command.date, command.key)
            is ScheduleWorkCommand.CreateSchedule -> createScheduleWork(command.date, command.plannedTemplates)
            is ScheduleWorkCommand.TimeTaskShiftDown -> shiftDownTimeWork(command.timeTask)
            is ScheduleWorkCommand.TimeTaskShiftUp -> shiftUpTimeWork(command.timeTask)
        }

        private suspend fun loadScheduleByDateWork(date: Date) = flow {
            scheduleInteractor.fetchScheduleByDate(date.time).collect { scheduleEither ->
                scheduleEither.handle(
                    onRightAction = { foundedDomainSchedule ->
                        when (foundedDomainSchedule) {
                            is Either.Right -> {
                                val schedule = foundedDomainSchedule.data.map(mapperToUi)
                                refreshScheduleState(schedule)
                            }
                            is Either.Left -> {
                                val foundedRepeatTasks = foundedDomainSchedule.data.map { it.mapToUi() }
                                emit(ActionResult(HomeAction.SetEmptySchedule(date, null, foundedRepeatTasks)))
                            }
                        }
                    },
                    onLeftAction = { error -> emit(EffectResult(HomeEffect.ShowError(error))) },
                )
            }
        }
        
        private suspend fun FlowCollector<WorkResult<HomeAction, HomeEffect>>.refreshScheduleState(
            schedule: ScheduleUi,
        ) {
            var oldTimeTasks = schedule.timeTasks
            var isWorking = true
            while (isWorking) {
                val newTimeTasks = oldTimeTasks.map { statusController.updateStatus(it) }
                if (newTimeTasks != oldTimeTasks || schedule.timeTasks == oldTimeTasks) {
                    oldTimeTasks = newTimeTasks
                    val newSchedule = schedule.copy(timeTasks = oldTimeTasks)
                    emit(ActionResult(HomeAction.UpdateSchedule(newSchedule)))
                    if (newTimeTasks.find { it.executionStatus != TimeTaskStatus.COMPLETED } != null) {
                        scheduleInteractor.updateSchedule(newSchedule.mapToDomain())
                    }
                }
                if (isWorking) delay(Constants.Delay.CHECK_STATUS)
                isWorking = oldTimeTasks.find { it.executionStatus != TimeTaskStatus.COMPLETED } != null
            }
        }

        private fun changeTaskDoneStateWork(date: Date, key: Long) = flow {
            val schedule = scheduleInteractor.fetchScheduleByDate(date.time).firstOrNull()?.rightOrElse(null)
            if (schedule is Either.Right) {
                val timeTasks = schedule.data.timeTasks.toMutableList().apply {
                    val oldTimeTaskIndex = indexOfFirst { it.key == key }
                    val oldTimeTask = get(oldTimeTaskIndex)
                    val newTimeTask = oldTimeTask.copy(isCompleted = !oldTimeTask.isCompleted)
                    set(oldTimeTaskIndex, newTimeTask)
                }
                val newSchedule = schedule.data.copy(timeTasks = timeTasks)
                scheduleInteractor.updateSchedule(newSchedule).handle(
                    onLeftAction = { emit(EffectResult(HomeEffect.ShowError(it))) },
                )
            }
        }

        private suspend fun createScheduleWork(date: Date, plannedTemplates: List<TemplateUi>) = flow {
            val plannedTimeTasks = plannedTemplates.map { it.mapToDomain().convertToTimeTask(date) }
            scheduleInteractor.createSchedule(date, plannedTimeTasks).handle(
                onRightAction = { addNotifications(plannedTimeTasks) },
                onLeftAction = { emit(EffectResult(HomeEffect.ShowError(it))) },
            )
        }

        private suspend fun shiftUpTimeWork(timeTask: TimeTaskUi) = flow {
            val shiftValue = Constants.Date.SHIFT_MINUTE_VALUE
            timeShiftInteractor.shiftUpTimeTask(timeTask.mapToDomain(), shiftValue).handle(
                onLeftAction = { emit(EffectResult(HomeEffect.ShowError(it))) },
            )
        }

        private suspend fun shiftDownTimeWork(timeTask: TimeTaskUi) = flow {
            val shiftValue = Constants.Date.SHIFT_MINUTE_VALUE
            timeShiftInteractor.shiftDownTimeTask(timeTask.mapToDomain(), shiftValue).handle(
                onLeftAction = { emit(EffectResult(HomeEffect.ShowError(it))) },
            )
        }

        private fun addNotifications(timeTasks: List<TimeTask>) {
            timeTasks.forEach { timeTask ->
                if (timeTask.isEnableNotification) {
                    val currentTime = dateManager.fetchCurrentDate()
                    if (timeTask.timeRanges.from > currentTime && timeTask.key != 0L) {
                        timeTaskAlarmManager.addNotifyAlarm(timeTask)
                    }
                } 
            }
        }
    }
}

internal sealed class ScheduleWorkCommand : WorkCommand {
    data class LoadScheduleByDate(val date: Date) : ScheduleWorkCommand()
    data class CreateSchedule(val date: Date, val plannedTemplates: List<TemplateUi>) : ScheduleWorkCommand()
    data class ChangeTaskDoneState(val date: Date, val key: Long) : ScheduleWorkCommand()
    data class TimeTaskShiftUp(val timeTask: TimeTaskUi) : ScheduleWorkCommand()
    data class TimeTaskShiftDown(val timeTask: TimeTaskUi) : ScheduleWorkCommand()
}
