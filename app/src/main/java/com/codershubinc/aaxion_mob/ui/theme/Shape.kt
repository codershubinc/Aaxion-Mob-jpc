package com.codershubinc.aaxion_mob.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Strict border-radius scale for uniform component corners
val CornerRadiusSmall = 8.dp
val CornerRadiusMedium = 12.dp
val CornerRadiusLarge = 16.dp

val Shapes = Shapes(
    small = RoundedCornerShape(CornerRadiusSmall),
    medium = RoundedCornerShape(CornerRadiusMedium),
    large = RoundedCornerShape(CornerRadiusLarge)
)
