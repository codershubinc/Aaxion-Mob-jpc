package com.codershubinc.aaxion_mob.feature.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codershubinc.aaxion_mob.ui.designsystem.GlassButton
import com.codershubinc.aaxion_mob.ui.theme.*

@Composable
fun MusicHeaderActions(
    trackCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
    currentPath: String = "",
    onAddTrack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = LocalAccentColor.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Hero Play Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Shuffle All" primary button
            GlassButton(
                text = if (trackCount > 0) "Shuffle ($trackCount)" else "Shuffle All",
                onClick = onShuffleAll,
                icon = Icons.Default.Shuffle,
                isPrimary = true,
                cornerRadius = 12.dp,
                accentColor = accentColor,
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp)
            )

            // "Play All" sequential button
            GlassButton(
                text = "Play All",
                onClick = onPlayAll,
                icon = Icons.Default.PlayArrow,
                isPrimary = false,
                cornerRadius = 12.dp,
                accentColor = accentColor,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )

            if (onAddTrack != null) {
                GlassButton(
                    text = "Add",
                    onClick = onAddTrack,
                    icon = Icons.Default.Add,
                    isPrimary = false,
                    cornerRadius = 12.dp,
                    accentColor = accentColor,
                    modifier = Modifier.height(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar with instant filter
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = "Filter songs by name...",
                    color = TextTertiary,
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = AaxionCardBg,
                unfocusedContainerColor = AaxionCardBg,
                disabledContainerColor = AaxionCardBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .border(0.5.dp, GlassHairline, RoundedCornerShape(12.dp))
        )
    }
}
