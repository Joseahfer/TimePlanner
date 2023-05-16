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
package ru.aleshin.features.editor.impl.presentation.ui.editor.views

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.aleshin.core.ui.theme.TimePlannerRes
import ru.aleshin.core.ui.views.DialogButtons
import ru.aleshin.features.editor.impl.presentation.theme.EditorThemeRes
import ru.aleshin.features.home.api.domains.entities.categories.MainCategory
import ru.aleshin.features.home.api.domains.entities.categories.SubCategory
import ru.aleshin.features.home.api.presentation.mappers.fetchNameByLanguage

/**
 * @author Stanislav Aleshin on 26.02.2023.
 */
@Composable
internal fun SubCategoryChooser(
    modifier: Modifier = Modifier,
    mainCategory: MainCategory?,
    allSubCategories: List<SubCategory>,
    currentSubCategory: SubCategory?,
    onSubCategoryChange: (SubCategory?) -> Unit,
    onManageCategories: () -> Unit,
) {
    val openDialog = rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = { openDialog.value = true },
        modifier = modifier.sizeIn(minHeight = 68.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = TimePlannerRes.elevations.levelOne,
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).animateContentSize()) {
                val mainTitle = if (mainCategory != null) currentSubCategory?.name else ""
                Text(
                    text = EditorThemeRes.strings.subCategoryChooserTitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = mainTitle ?: EditorThemeRes.strings.categoryNotSelectedTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            val icon = when (currentSubCategory != null) {
                true -> EditorThemeRes.icons.showDialog
                false -> EditorThemeRes.icons.add
            }
            val tint = when (mainCategory != null) {
                true -> MaterialTheme.colorScheme.onSurface
                false -> MaterialTheme.colorScheme.surfaceVariant
            }
            Icon(
                modifier = Modifier.animateContentSize(),
                painter = painterResource(icon),
                contentDescription = EditorThemeRes.strings.mainCategoryChooserExpandedIconDesc,
                tint = tint,
            )
        }
    }
    if (openDialog.value) {
        SubCategoryDialogChooser(
            initCategory = currentSubCategory,
            mainCategory = mainCategory,
            allSubCategories = allSubCategories,
            onCloseDialog = { openDialog.value = false },
            onManageCategories = {
                onManageCategories()
                openDialog.value = false
            },
            onChooseSubCategory = {
                onSubCategoryChange(it)
                openDialog.value = false
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SubCategoryDialogChooser(
    modifier: Modifier = Modifier,
    initCategory: SubCategory?,
    mainCategory: MainCategory?,
    allSubCategories: List<SubCategory>,
    onCloseDialog: () -> Unit,
    onChooseSubCategory: (SubCategory?) -> Unit,
    onManageCategories: () -> Unit,
) {
    val initItem = initCategory?.let { allSubCategories.find { it.id == initCategory.id } }
    val initPosition = initItem?.let { allSubCategories.indexOf(it) } ?: 0
    val listState = rememberLazyListState(initPosition)
    var selectedSubCategory by rememberSaveable { mutableStateOf(initCategory) }

    AlertDialog(onDismissRequest = onCloseDialog) {
        Surface(
            modifier = modifier.width(280.dp).wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = TimePlannerRes.elevations.levelThree,
        ) {
            Column {
                Column(
                    modifier = Modifier.padding(
                        top = 24.dp,
                        bottom = 8.dp,
                        start = 24.dp,
                        end = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = EditorThemeRes.strings.subCategoryChooserTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        text = EditorThemeRes.strings.subCategoryDialogMainCategoryFormat.format(
                            mainCategory?.fetchNameByLanguage() ?: EditorThemeRes.strings.categoryNotSelectedTitle,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                LazyColumn(modifier = Modifier.height(300.dp), state = listState) {
                    item {
                        SubCategoryDialogItem(
                            selected = selectedSubCategory == null,
                            title = EditorThemeRes.strings.categoryNotSelectedTitle,
                            description = null,
                            onSelectChange = { selectedSubCategory = null },
                        )
                    }
                    items(allSubCategories) { subCategory ->
                        SubCategoryDialogItem(
                            selected = selectedSubCategory == subCategory,
                            title = subCategory.name,
                            description = subCategory.description,
                            onSelectChange = { selectedSubCategory = subCategory },
                        )
                    }
                    item {
                        ManageCategoriesDialogItem(
                            modifier = Modifier.fillMaxWidth(),
                            onManage = onManageCategories,
                        )
                    }
                }
                DialogButtons(
                    onCancelClick = onCloseDialog,
                    onConfirmClick = { onChooseSubCategory.invoke(selectedSubCategory) },
                )
            }
        }
    }
}

@Composable
internal fun SubCategoryDialogItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    title: String,
    description: String?,
    onSelectChange: () -> Unit,
) {
    Column {
        Row(
            modifier = modifier
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onSelectChange)
                .padding(start = 8.dp, end = 16.dp)
                .requiredHeight(56.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (description != null) {
                    Text(
                        text = description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Divider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
internal fun ManageCategoriesDialogItem(
    modifier: Modifier = Modifier,
    onManage: () -> Unit,
) {
    Column {
        Row(
            modifier = modifier
                .padding(vertical = 8.dp, horizontal = 24.dp).height(48.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onManage),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Default.Create,
                contentDescription = EditorThemeRes.strings.subCategoryDialogManageTitle,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = EditorThemeRes.strings.subCategoryDialogManageTitle,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Divider(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/* ----------------------- Release Preview -----------------------
@Composable
@Preview(showBackground = true)
private fun SubCategoryDialogChooser_Preview() {
    TimePlannerTheme(
        dynamicColor = false,
        themeColorsType = ThemeColorsUiType.DARK,
    ) {
        EditorTheme {
            SubCategoryDialogChooser(
                onCloseDialog = { },
                mainCategory = MainCategory(englishName = "Work", name = "Работа"),
                allSubCategories = emptyList(),
                initCategory = null,
                onChooseSubCategory = {},
                onAddSubCategory = {},
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun SubCategoryChooser_Preview() {
    TimePlannerTheme(dynamicColor = false, themeColorsType = ThemeColorsUiType.DARK) {
        EditorTheme {
            val category = rememberSaveable { mutableStateOf<SubCategory?>(null) }
            SubCategoryChooser(
                modifier = Modifier.fillMaxWidth(),
                mainCategory = MainCategory(englishName = "Work", name = "Работа"),
                allSubCategories = emptyList(),
                currentSubCategory = category.value,
                onSubCategoryChoose = {},
                onAddSubCategory = {},
            )
        }
    }
}
*/