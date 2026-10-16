package com.codershubinc.aaxion_mob.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Strict border-radius scale for uniform component corners
val CornerRadiusSmall = 10.dp
val CornerRadiusMedium = 14.dp
val CornerRadiusLarge = 20.dp

val Shapes = Shapes(
    small = RoundedCornerShape(CornerRadiusSmall),
    medium = RoundedCornerShape(CornerRadiusMedium),
    large = RoundedCornerShape(CornerRadiusLarge)
)
