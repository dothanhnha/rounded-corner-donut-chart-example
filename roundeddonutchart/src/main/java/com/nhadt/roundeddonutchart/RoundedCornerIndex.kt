package com.nhadt.roundeddonutchart

import android.graphics.PointF

class RoundedCornerIndex(
    val cornerRoundedCoordinate: CornerRoundedCoordinate,
    val centerRounded: PointF,
    val roundedRadius: Float,
    val startSweep: Float,
    val sweep: Float
)