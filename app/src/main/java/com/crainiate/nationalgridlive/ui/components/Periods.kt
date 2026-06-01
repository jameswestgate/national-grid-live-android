package com.crainiate.nationalgridlive.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.crainiate.nationalgridlive.data.model.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSelector(
    selected: Period,
    onSelect: (Period) -> Unit,
    modifier: Modifier = Modifier
) {
    val periods = Period.entries
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        periods.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
                label = {
                    Text(
                        period.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            )
        }
    }
}
