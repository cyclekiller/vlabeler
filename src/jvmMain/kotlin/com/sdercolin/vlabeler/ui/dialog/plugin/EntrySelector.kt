package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.common.InputBox
import com.sdercolin.vlabeler.ui.common.SelectionBox
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.JavaScript
import com.sdercolin.vlabeler.util.runIf

@Composable
fun ParamEntrySelector(
    labelerConf: LabelerConf?,
    value: EntrySelector,
    onValueChange: (EntrySelector) -> Unit,
    isError: Boolean,
    onParseErrorChange: (Boolean) -> Unit,
    entries: List<Entry>?,
    js: JavaScript?,
    enabled: Boolean,
) {
    val filters = remember(value) { mutableStateListOf(*value.filters.toTypedArray()) }
    val parseErrors = remember(value) { mutableStateListOf(*Array(value.filters.size) { false }) }
    val verticalScrollState = rememberLazyListState()
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colors.background),
    ) {
        Row(Modifier.height(160.dp).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                LazyColumn(state = verticalScrollState, modifier = Modifier.padding(15.dp)) {
                    itemsIndexed(filters) { index, filter ->
                        FilterRow(
                            labelerConf,
                            index,
                            filter,
                            onValueChange = {
                                filters[index] = it
                                onValueChange(EntrySelector(filters.toList()))
                            },
                            onParseErrorChange = { isError ->
                                parseErrors[index] = isError
                                onParseErrorChange(parseErrors.any { it })
                            },
                            enabled = enabled,
                        )
                    }
                }
                if (filters.isEmpty()) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            text = string(Strings.PluginEntrySelectorPlaceholder),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.body2,
                        )
                    }
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(verticalScrollState))
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.surface).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                modifier = Modifier.size(18.dp).clickable(enabled = enabled) {
                    val newItem = EntrySelector.TextFilterItem(
                        subject = EntrySelector.textItemSubjects.first().first,
                        matchType = EntrySelector.TextMatchType.values().first(),
                        matcherText = "",
                    )
                    onValueChange(EntrySelector(filters.toList().plus(newItem)))
                },
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.runIf(enabled.not()) { copy(alpha = 0.2f) },
            )
            val minusButtonEnabled = enabled && filters.isNotEmpty()
            Icon(
                modifier = Modifier.size(18.dp).clickable(enabled = minusButtonEnabled) {
                    onValueChange(EntrySelector(filters.toList().dropLast(1)))
                },
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.runIf(minusButtonEnabled.not()) { copy(alpha = 0.2f) },
            )
            Spacer(Modifier.weight(1f))
            if (isError || parseErrors.any { it }) {
                Text(
                    text = string(Strings.PluginEntrySelectorPreviewSummaryError),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.error,
                )
            } else {
                val text = if (js == null) {
                    string(Strings.PluginEntrySelectorPreviewSummaryInitializing)
                } else if (entries != null && labelerConf != null) {
                    val selectedCount = EntrySelector(filters.toList()).select(entries, labelerConf, js).size
                    string(Strings.PluginEntrySelectorPreviewSummary, selectedCount, entries.size)
                } else {
                    ""
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface,
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    labelerConf: LabelerConf?,
    index: Int,
    value: EntrySelector.FilterItem,
    onValueChange: (EntrySelector.FilterItem) -> Unit,
    onParseErrorChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    var type by remember(value) { mutableStateOf(value::class) }
    var subject by remember(value) { mutableStateOf(value.subject) }
    val textSubjects = EntrySelector.textItemSubjects.map { it.first to string(it.second) }
    val textSubjectNames = textSubjects.map { it.first }
    val numberSubjects = labelerConf?.properties?.map { it.name to it.displayedName.get() }.orEmpty()
    val numberComparers = listOf(null to string(Strings.PluginEntrySelectorComparerValue)) +
        labelerConf?.properties?.map { it.name to it.displayedName.get() }.orEmpty()
    val booleanSubjects = EntrySelector.booleanItemSubjects.map { it.first to string(it.second) }
    val booleanSubjectNames = booleanSubjects.map { it.first }
    val subjects = textSubjects + numberSubjects + booleanSubjects
    var textMatchType by remember(value) {
        mutableStateOf(
            (value as? EntrySelector.TextFilterItem)?.matchType
                ?: EntrySelector.TextMatchType.values().first(),
        )
    }
    var textMatchValue by remember(value) {
        mutableStateOf(
            (value as? EntrySelector.TextFilterItem)?.matcherText ?: "",
        )
    }
    var numberMatchType by remember(value) {
        mutableStateOf(
            (value as? EntrySelector.NumberFilterItem)?.matchType
                ?: EntrySelector.NumberMatchType.values().first(),
        )
    }
    var numberComparerValue by remember {
        mutableStateOf(
            (value as? EntrySelector.NumberFilterItem)?.comparerValue?.toString().orEmpty(),
        )
    }
    var booleanMatchValue by remember(value) {
        mutableStateOf(
            (value as? EntrySelector.BooleanFilterItem)?.matcherBoolean ?: false,
        )
    }
    LaunchedEffect(value) {
        val newNumberComparerValue = (value as? EntrySelector.NumberFilterItem)?.comparerValue
        if (numberComparerValue.toDoubleOrNull() != newNumberComparerValue) {
            numberComparerValue = newNumberComparerValue?.toString().orEmpty()
        }
    }
    var numberMatchComparerName by remember(value) {
        mutableStateOf((value as? EntrySelector.NumberFilterItem)?.comparerName)
    }

    fun trySubmit() {
        val newValue: EntrySelector.FilterItem = when (type) {
            EntrySelector.NumberFilterItem::class -> {
                val comparerValue = numberComparerValue.toDoubleOrNull()
                if (comparerValue == null && numberMatchComparerName == null) {
                    onParseErrorChange(true)
                    return
                }
                EntrySelector.NumberFilterItem(
                    subject,
                    numberMatchType,
                    comparerValue ?: 0.0,
                    numberMatchComparerName,
                )
            }
            EntrySelector.TextFilterItem::class -> EntrySelector.TextFilterItem(
                subject,
                textMatchType,
                textMatchValue,
            )
            EntrySelector.BooleanFilterItem::class -> EntrySelector.BooleanFilterItem(
                subject,
                booleanMatchValue,
            )
            else -> return
        }
        onValueChange(newValue)
        onParseErrorChange(false)
    }

    Row(
        modifier = Modifier.fillMaxSize().padding(vertical = 5.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "#${index + 1}",
            color = MaterialTheme.colors.primary,
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            modifier = Modifier.width(30.dp),
        )
        SelectionBox(
            value = subjects.firstOrNull { it.first == subject } ?: (subject to subject),
            onSelect = {
                subject = it.first
                type = when (it.first) {
                    in textSubjectNames -> EntrySelector.TextFilterItem::class
                    in booleanSubjectNames -> EntrySelector.BooleanFilterItem::class
                    else -> EntrySelector.NumberFilterItem::class
                }
                trySubmit()
            },
            options = subjects,
            getText = { it.second },
            modifier = Modifier.width(160.dp),
            fixedWidth = true,
            showIcon = false,
            enabled = enabled,
        )
        when (type) {
            EntrySelector.NumberFilterItem::class -> {
                SelectionBox(
                    value = numberMatchType,
                    onSelect = {
                        numberMatchType = it
                        trySubmit()
                    },
                    options = EntrySelector.NumberMatchType.values().toList(),
                    getText = { string(it.strings) },
                    modifier = Modifier.width(60.dp),
                    fixedWidth = true,
                    showIcon = false,
                    enabled = enabled,
                )
                SelectionBox(
                    value = numberComparers.firstOrNull { it.first == numberMatchComparerName }
                        ?: numberComparers.first(),
                    onSelect = {
                        numberMatchComparerName = it.first
                        trySubmit()
                    },
                    options = numberComparers,
                    getText = { it.second },
                    modifier = Modifier.width(120.dp),
                    fixedWidth = true,
                    showIcon = false,
                    enabled = enabled,
                )
                InputBox(
                    value = numberComparerValue,
                    onValueChange = {
                        numberComparerValue = it
                        trySubmit()
                    },
                    modifier = Modifier.width(80.dp),
                    enabled = enabled && (numberMatchComparerName == null),
                    errorPrompt = { if (numberComparerValue.toDoubleOrNull() == null) "" else null },
                )
            }
            EntrySelector.TextFilterItem::class -> {
                SelectionBox(
                    value = textMatchType,
                    onSelect = {
                        textMatchType = it
                        trySubmit()
                    },
                    options = EntrySelector.TextMatchType.values().toList(),
                    getText = { string(it.strings) },
                    modifier = Modifier.width(120.dp),
                    fixedWidth = true,
                    showIcon = false,
                    enabled = enabled,
                )
                InputBox(
                    value = textMatchValue,
                    onValueChange = {
                        textMatchValue = it
                        trySubmit()
                    },
                    modifier = Modifier.width(160.dp),
                    errorPrompt = { if (textMatchValue.isEmpty()) "" else null },
                    enabled = enabled,
                )
            }
            EntrySelector.BooleanFilterItem::class -> {
                Switch(
                    checked = booleanMatchValue,
                    onCheckedChange = {
                        booleanMatchValue = it
                        trySubmit()
                    },
                    colors = getSwitchColors(),
                    enabled = enabled,
                )
            }
        }
    }
}
