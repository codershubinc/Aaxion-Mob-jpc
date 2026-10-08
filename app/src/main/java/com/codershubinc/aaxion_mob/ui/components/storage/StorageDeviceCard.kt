package com.codershubinc.aaxion_mob.ui.components.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.codershubinc.aaxion_mob.ui.components.AaxionCard
import com.codershubinc.aaxion_mob.ui.theme.AaxionPrimary
import com.codershubinc.aaxion_mob.ui.theme.AaxionSurfaceVariant
import com.codershubinc.aaxion_mob.ui.theme.TextPrimary
import com.codershubinc.aaxion_mob.ui.theme.TextSecondary

@Composable
fun StorageDeviceCard(
    deviceName: String,
    totalStorageGb: Double,
    usedStorageGb: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val usagePercentage = (usedStorageGb / totalStorageGb).toFloat().coerceIn(0f, 1f)
    
    AaxionCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = AaxionSurfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = AaxionPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { usagePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = AaxionPrimary,
                trackColor = TextSecondary.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${"%.1f".format(usedStorageGb)}GB / ${"%.1f".format(totalStorageGb)}GB Used",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
