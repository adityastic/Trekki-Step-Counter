package com.adityagupta.trekki

import kotlin.math.sqrt

class Accelerometer(event: FloatArray?) {
    var X: Float = event?.get(0) ?: 0f
    var Y: Float = event?.get(1) ?: 0f
    var Z: Float = event?.get(2) ?: 0f
    var R: Double

    fun toNumber(): Number {
        return R
    }

    init {
        R = sqrt(X * X + Y * Y + (Z * Z).toDouble())
    }
}