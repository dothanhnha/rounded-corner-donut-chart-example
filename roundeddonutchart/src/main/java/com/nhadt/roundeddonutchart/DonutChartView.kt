package com.nhadt.roundeddonutchart

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class DonutChartView(context: Context, attrs: AttributeSet?) : View(
    context,
    attrs
) {

    companion object {
        const val OFFSET_MINIMUM_ROUNDED = 4F
    }

    private lateinit var innerRectF: RectF
    private var radius: Float = 0F
    private lateinit var boundary: RectF
    private var centerX: Float = 0F
    private var centerY: Float = 0F
    private var notTooSmallForOuterRounded: Float = 0F
    private var notTooSmallForInnerRounded: Float = 0F
    private var paints: List<Paint> = emptyList()
    private var percentages: List<Float> = emptyList()
    private var gapAngle = 4f
    private var strokeWidth = 200f
    private var radiusRounded = 20F
    private var strokeWidthPercentRadius = 48 / 100F
    private var startAngle = 90f

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DonutChartView,
            0, 0
        ).apply {
            try {
                gapAngle = getFloat(
                    R.styleable.DonutChartView_gapAngle,
                    gapAngle
                )
                radiusRounded = getDimensionPixelSize(
                    R.styleable.DonutChartView_radiusRoundedDP,
                    50
                ).toFloat()
                strokeWidthPercentRadius = getFloat(
                    R.styleable.DonutChartView_percentStrokeWidthOnRadius,
                    3 / 10F
                ).toFloat()
                startAngle = getFloat(
                    R.styleable.DonutChartView_startAngle,
                    startAngle
                )
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        centerX = width / 2f
        centerY = height / 2f
        radius = (width.coerceAtMost(height) / 2f)
        strokeWidth = strokeWidthPercentRadius * radius
        boundary = RectF(
            centerX - radius, centerY - radius, centerX + radius, centerY + radius
        )
        innerRectF = RectF(
            centerX - (radius - strokeWidth), centerY - (radius - strokeWidth),
            centerX + (radius - strokeWidth), centerY + (radius - strokeWidth)
        )
        val radiusForCalculateMinimum = radiusRounded + OFFSET_MINIMUM_ROUNDED
        notTooSmallForOuterRounded = Math.toDegrees(
            acos(
                1F - 2 * radiusForCalculateMinimum.pow(2) / (radius - radiusForCalculateMinimum).pow(
                    2
                )
            ).toDouble()
        ).toFloat()

        notTooSmallForInnerRounded =
            Math.toDegrees(
                acos(
                    1F - 2 * radiusForCalculateMinimum.pow(2) / ((radius - strokeWidth + 2 * radiusForCalculateMinimum) - radiusForCalculateMinimum).pow(
                        2
                    )
                ).toDouble()
            ).toFloat()

    }

    // For monochrome
    fun setData(data: List<Float>, solidColors: List<Int>) {
        percentages = data
        paints = solidColors.map { color ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                this.color = color
            }
        }
        invalidate()
    }

    fun setData(data: List<Float>, solidColor: Int) {
        percentages = data
        paints = List(data.size) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                this@apply.color = solidColor
            }
        }
        invalidate()
    }

    // For gradient colors
    fun setDataHaveGradient(data: List<Float>, gradientColors: List<Pair<Int, Int>>) {
        this.post {
            percentages = data
            paints = gradientColors.map { colorPair ->
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    if (width > 0 && height > 0) {
                        this@apply.shader = LinearGradient(
                            width / 2F, height.toFloat(), width / 2F, 0F,
                            colorPair.first,
                            colorPair.second,
                            Shader.TileMode.CLAMP
                        )
                    }
                }
            }
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (percentages.isEmpty()) {
            return // Nothing to draw
        }

        percentages.forEachIndexed { index, percentage ->
            val sweepAngle = (percentage / 100f) * 360f - gapAngle

            if (sweepAngle > 0) { // Don't draw segments with zero or negative sweep angle

                val startRad = startAngle.toRadian()
                val endRad = (startAngle + sweepAngle).toRadian()

                // Outer arc point:
                val outerX = centerX + radius * cos(startRad)
                val outerY = centerY + radius * sin(startRad)

                //Draw a line to the inner arc:
                val innerEndX = centerX + (radius - strokeWidth) * cos(endRad)
                val innerEndY = centerY + (radius - strokeWidth) * sin(endRad)

                drawSegment(
                    PointF(centerX, centerY),
                    canvas,
                    outerX,
                    outerY,
                    innerEndX,
                    innerEndY,
                    innerRectF,
                    boundary,
                    startAngle,
                    sweepAngle,
                    radius,
                    radiusInner = radius - strokeWidth,
                    index
                )

                startAngle += sweepAngle + gapAngle
            } else {
                startAngle += gapAngle // Skip this segment, just advance the start angle
            }
        }
    }

    private fun drawSegment(
        center: PointF,
        canvas: Canvas,
        outerX: Float,
        outerY: Float,
        innerEndX: Float,
        innerEndY: Float,
        innerRectF: RectF,
        bounder: RectF,
        startAngle: Float,
        sweepAngle: Float,
        radius: Float,
        radiusInner: Float,
        index: Int
    ) {
        val innerEndSweep = PointF(innerEndX.toFloat(), innerEndY.toFloat())
        val outerEndSweep =
            PointF(outerX.toFloat(), outerY.toFloat()).rotate(sweepAngle, center.x, center.y)
        val innerStartSweep =
            PointF(innerEndX.toFloat(), innerEndY.toFloat()).rotate(-sweepAngle, center.x, center.y)
        val outerStartSweep = PointF(outerX.toFloat(), outerY.toFloat())

        val segmentRoundedCorners = calculateRoundedSegment(
            center,
            radius,
            radiusInner,
            startAngle,
            sweepAngle,
            outerStartSweep,
            innerStartSweep,
            outerEndSweep,
            innerEndSweep,
            radiusRounded
        )
        segmentRoundedCorners.outerStartSweep?.let { roundedOuterStartSweepIndex ->
            segmentRoundedCorners.innerStartSweep?.let { roundedInnerStartSweepIndex ->
                segmentRoundedCorners.outerEndSweep?.let { roundedOuterEndSweepIndex ->
                    segmentRoundedCorners.innerEndSweep?.let { roundedInnerEndSweepIndex ->
                        val outerTooSmall = sweepAngle <= notTooSmallForOuterRounded
                        val innerTooSmall = sweepAngle <= notTooSmallForInnerRounded
                        var roundedSegmentTooSmallOuter: CircleRoundedForTooSmallCase? = null
                        if (outerTooSmall) {
                            roundedSegmentTooSmallOuter = calculateRoundedSegmentTooSmall(
                                center = center,
                                radius = radius,
                                radiusInner = radiusInner,
                                sweepAngle = sweepAngle,
                                outerStartSweep = outerStartSweep,
                                outerEndSweep = outerEndSweep,
                                isOuterTooSmall = true
                            )
                        }
                        var roundedSegmentTooSmallInner: CircleRoundedForTooSmallCase? = null
                        if (innerTooSmall) {
                            roundedSegmentTooSmallInner = calculateRoundedSegmentTooSmall(
                                center = center,
                                radius = radius,
                                radiusInner = radiusInner,
                                sweepAngle = sweepAngle,
                                outerStartSweep = outerStartSweep,
                                outerEndSweep = outerEndSweep,
                                isOuterTooSmall = false
                            )
                        }

                        val startAngleNew = calculatePositiveAngle(
                            center,
                            roundedOuterStartSweepIndex.cornerRoundedCoordinate.pointOnCurve
                        )
                        val sweepAngleNew = calculateClockwiseAngle(
                            roundedOuterStartSweepIndex.cornerRoundedCoordinate.pointOnCurve,
                            center,
                            roundedOuterEndSweepIndex.cornerRoundedCoordinate.pointOnCurve
                        )
                        val path = Path()
                        ////outer curve
                        roundedSegmentTooSmallOuter?.let { tooSmallOuter ->
                            path.moveTo(
                                tooSmallOuter.startPointWhenDrawRounded.x,
                                tooSmallOuter.startPointWhenDrawRounded.y
                            )
                            path.arcTo(
                                getRectWrapCircle(
                                    tooSmallOuter.center,
                                    tooSmallOuter.radius
                                ), tooSmallOuter.startAngle, tooSmallOuter.sweep, false
                            )
                        } ?: run {
                            path.moveTo(
                                roundedOuterStartSweepIndex.cornerRoundedCoordinate.pointOnCurve.x,
                                roundedOuterStartSweepIndex.cornerRoundedCoordinate.pointOnCurve.y
                            )  // Start at the outer arc point
                            // Draw outer arc:
                            path.arcTo(bounder, startAngleNew, sweepAngleNew, false)
                            path.arcTo(
                                getRectWrapCircle(
                                    roundedOuterEndSweepIndex.centerRounded,
                                    radiusRounded
                                ),
                                roundedOuterEndSweepIndex.startSweep,
                                roundedOuterEndSweepIndex.sweep,
                                false
                            )
                        }
                        /////////////////////////
                        /////line end sweep:
                        roundedSegmentTooSmallInner?.let { tooSmallInner ->
                            path.lineTo(
                                tooSmallInner.startPointWhenDrawRounded.x,
                                tooSmallInner.startPointWhenDrawRounded.y
                            )
                        } ?: run {
                            path.lineTo(
                                roundedInnerEndSweepIndex.cornerRoundedCoordinate.pointOnLine.x,
                                roundedInnerEndSweepIndex.cornerRoundedCoordinate.pointOnLine.y,
                            )
                            path.arcTo(
                                getRectWrapCircle(
                                    roundedInnerEndSweepIndex.centerRounded,
                                    radiusRounded
                                ),
                                roundedInnerEndSweepIndex.startSweep,
                                roundedInnerEndSweepIndex.sweep,
                                false
                            )
                        }
                        /////////////////////////
                        ///// Draw inner arc:
                        roundedSegmentTooSmallInner?.let { tooSmallInner ->
                            path.arcTo(
                                getRectWrapCircle(
                                    tooSmallInner.center,
                                    tooSmallInner.radius
                                ), tooSmallInner.startAngle, tooSmallInner.sweep, false
                            )
                        } ?: run {
                            path.arcTo(
                                innerRectF,
                                startAngleNew + sweepAngleNew,
                                -sweepAngleNew,
                                false
                            )
                            path.arcTo(
                                getRectWrapCircle(
                                    roundedInnerStartSweepIndex.centerRounded,
                                    radiusRounded
                                ),
                                roundedInnerStartSweepIndex.startSweep,
                                roundedInnerStartSweepIndex.sweep,
                                false
                            )
                        }
                        /////////////////////////
                        /////line start sweep:
                        roundedSegmentTooSmallOuter?.let { tooSmallOuter ->
                            path.lineTo(
                                tooSmallOuter.startPointWhenDrawRounded.x,
                                tooSmallOuter.startPointWhenDrawRounded.y,
                            )
                        } ?: run {
                            path.lineTo(
                                roundedOuterStartSweepIndex.cornerRoundedCoordinate.pointOnLine.x,
                                roundedOuterStartSweepIndex.cornerRoundedCoordinate.pointOnLine.y
                            )
                            path.arcTo(
                                getRectWrapCircle(
                                    roundedOuterStartSweepIndex.centerRounded,
                                    radiusRounded
                                ),
                                roundedOuterStartSweepIndex.startSweep,
                                roundedOuterStartSweepIndex.sweep,
                                false
                            )
                        }

                        path.close()
                        paints.elementAtOrNull(index)?.let {
                            canvas.drawPath(path, it)
                        }
                        val oldPath = Path()
                        oldPath.moveTo(
                            segmentRoundedCorners.innerStartSweep.cornerRoundedCoordinate.pointOnCurve.x,
                            segmentRoundedCorners.innerStartSweep.cornerRoundedCoordinate.pointOnCurve.y,
                        )
                        oldPath.arcTo(
                            getRectWrapCircle(
                                roundedInnerStartSweepIndex.centerRounded,
                                radiusRounded
                            ),
                            roundedInnerStartSweepIndex.startSweep,
                            roundedInnerStartSweepIndex.sweep,
                            false
                        )
                        oldPath.lineTo(
                            segmentRoundedCorners.outerStartSweep.cornerRoundedCoordinate.pointOnLine.x,
                            segmentRoundedCorners.outerStartSweep.cornerRoundedCoordinate.pointOnLine.y,
                        )
                        oldPath.arcTo(
                            getRectWrapCircle(
                                roundedOuterStartSweepIndex.centerRounded,
                                radiusRounded
                            ),
                            roundedOuterStartSweepIndex.startSweep,
                            roundedOuterStartSweepIndex.sweep,
                            false
                        )
                        oldPath.arcTo(bounder, startAngleNew, sweepAngleNew)
                        oldPath.arcTo(
                            getRectWrapCircle(
                                roundedOuterEndSweepIndex.centerRounded,
                                radiusRounded
                            ),
                            roundedOuterEndSweepIndex.startSweep,
                            roundedOuterEndSweepIndex.sweep,
                            false
                        )
                        oldPath.lineTo(
                            segmentRoundedCorners.innerEndSweep.cornerRoundedCoordinate.pointOnLine.x,
                            segmentRoundedCorners.innerEndSweep.cornerRoundedCoordinate.pointOnLine.y,
                        )
                        oldPath.arcTo(
                            getRectWrapCircle(
                                roundedInnerEndSweepIndex.centerRounded,
                                radiusRounded
                            ),
                            roundedInnerEndSweepIndex.startSweep,
                            roundedInnerEndSweepIndex.sweep,
                            false
                        )
                        oldPath.arcTo(innerRectF, startAngleNew + sweepAngleNew, -sweepAngleNew)
                        oldPath.close()
                    }
                }
            }
        }
    }

    private fun calculateRoundedSegmentTooSmall(
        center: PointF,
        radius: Float,
        radiusInner: Float,
        sweepAngle: Float,
        outerStartSweep: PointF,
        outerEndSweep: PointF,
        isOuterTooSmall: Boolean
    ): CircleRoundedForTooSmallCase {
        val sweepAngleRad = sweepAngle.toRadian()
        val calculateRadius = if (isOuterTooSmall) {
            radius
        } else {
            radiusInner
        }
        val radiusRounded = if (isOuterTooSmall) {
            sin(sweepAngleRad / 2F) * calculateRadius / (1F + sin(sweepAngleRad / 2F))
        } else {
            sin(sweepAngleRad / 2F) * calculateRadius / (1F - sin(sweepAngleRad / 2F))
        }
        val spaceFromCenterToRoundedCorner = if (isOuterTooSmall) {
            radius - radiusRounded
        } else {
            radiusInner + radiusRounded
        }
        val centerRounded = pointAtDistanceOnLine(
            center,
            getMidPoint(outerStartSweep, outerEndSweep),
            spaceFromCenterToRoundedCorner
        )
        val startPointWhenDrawRounded = if (isOuterTooSmall) {
            findPerpendicularPointOnLine(center, outerStartSweep, centerRounded)
        } else {
            findPerpendicularPointOnLine(center, outerEndSweep, centerRounded)
        }
        return CircleRoundedForTooSmallCase(
            startPointWhenDrawRounded = startPointWhenDrawRounded,
            radius = radiusRounded,
            center = centerRounded,
            startAngle = calculatePositiveAngle(centerRounded, startPointWhenDrawRounded),
            sweep = if (isOuterTooSmall) {
                (90F + sweepAngle / 2F) * 2F
            } else {
                (90F - sweepAngle / 2F) * 2F
            }
        )
    }

    private fun calculateRoundedSegment(
        center: PointF,
        radius: Float,
        radiusInner: Float,
        startAngle: Float,
        sweepAngle: Float,
        outerStartSweep: PointF,
        innerStartSweep: PointF,
        outerEndSweep: PointF,
        innerEndSweep: PointF,
        radiusRounded: Float,
    ): SegmentRoundedCorners {
        val indexRoundedOuterStart = calculateRoundedCorner(
            center,
            radius,
            radiusInner,
            outerStartSweep,
            innerStartSweep,
            radiusRounded,
            true,
            false
        )
        val indexRoundedInnerStart = calculateRoundedCorner(
            center,
            radius,
            radiusInner,
            outerStartSweep,
            innerStartSweep,
            radiusRounded,
            false,
            false
        )

        val indexRoundedOuterEnd = calculateRoundedCorner(
            center,
            radius,
            radiusInner,
            outerEndSweep,
            innerEndSweep,
            radiusRounded,
            true,
            true
        )
        val indexRoundedInnerEnd = calculateRoundedCorner(
            center,
            radius,
            radiusInner,
            outerEndSweep,
            innerEndSweep,
            radiusRounded,
            false,
            true
        )
        return SegmentRoundedCorners(
            outerStartSweep = indexRoundedOuterStart,
            innerStartSweep = indexRoundedInnerStart,
            outerEndSweep = indexRoundedOuterEnd,
            innerEndSweep = indexRoundedInnerEnd,
        )
    }

    private fun calculateRoundedCorner(
        center: PointF,
        radius: Float,
        radiusInner: Float,
        outerPointCornerSweep: PointF,
        innerPointCornerSweep: PointF,
        radiusRounded: Float,
        isOuterRounded: Boolean,
        isEndSweep: Boolean
    ): RoundedCornerIndex? {
        val listParallelStartSweep =
            genParallelLines(outerPointCornerSweep, innerPointCornerSweep, radiusRounded)

        listParallelStartSweep.sortBy {
            val positiveAngle = calculatePositiveAngle(center, it.second)
            return@sortBy if (positiveAngle > 180F) {
                positiveAngle - 360F
            } else positiveAngle
        }

        val parallel = if (isEndSweep) {
            listParallelStartSweep.firstOrNull {
                !isClockwise(
                    center,
                    outerPointCornerSweep,
                    it.second
                )
            }
        } else {
            listParallelStartSweep.firstOrNull {
                isClockwise(
                    center,
                    outerPointCornerSweep,
                    it.second
                )
            }
        }
        parallel?.let { lineParallel ->
            findPerpendicularPointOnLine(
                lineParallel.first,
                lineParallel.second,
                center
            ).let { perpendicularPoint ->

                val space = findSpaceBetweenPerpendicularPointToCenterRounded(
                    center,
                    lineParallel.first,
                    lineParallel.second,
                    radiusRounded,
                    if (isOuterRounded) radius else radiusInner,
                    isOuterRounded
                )

                val centerRounded =
                    pointAtDistanceOnLine(perpendicularPoint, lineParallel.first, space)

                val cornerRoundedCoordinate = findTwoPointSweepRounded(
                    space,
                    center,
                    innerPointCornerSweep,
                    centerRounded,
                    if (isOuterRounded) radius else radiusInner
                )
                val sweepAngleRounded = if (isOuterRounded) {
                    calculateAngle(
                        cornerRoundedCoordinate.pointOnLine,
                        centerRounded,
                        cornerRoundedCoordinate.pointOnCurve
                    )
                } else {
                    calculateAngle(
                        cornerRoundedCoordinate.pointOnCurve,
                        centerRounded,
                        cornerRoundedCoordinate.pointOnLine,
                    )
                }
                val startAngleRounded =
                    if ((isEndSweep && isOuterRounded) || (!isEndSweep && !isOuterRounded)) {
                        calculatePositiveAngle(centerRounded, cornerRoundedCoordinate.pointOnCurve)
                    } else {
                        calculatePositiveAngle(centerRounded, cornerRoundedCoordinate.pointOnLine)
                    }

                return RoundedCornerIndex(
                    cornerRoundedCoordinate = cornerRoundedCoordinate,
                    centerRounded = centerRounded,
                    startSweep = startAngleRounded,
                    sweep = sweepAngleRounded
                )
            }
        }
        return null
    }

    fun PointF.rotate(angleDegrees: Float, xCenter: Float, yCenter: Float): PointF {
        val angleRadians = angleDegrees.toRadian()
        val rotatedX =
            ((x - xCenter) * cos(angleRadians) - (y - yCenter) * sin(angleRadians) + xCenter).toFloat()
        val rotatedY =
            ((x - xCenter) * sin(angleRadians) + (y - yCenter) * cos(angleRadians) + yCenter).toFloat()
        return PointF(rotatedX, rotatedY)
    }

    fun Pair<PointF, PointF>.rotate(
        angleDegrees: Float,
        xCenter: Float,
        yCenter: Float
    ): Pair<PointF, PointF> {
        return Pair(
            first.rotate(angleDegrees, xCenter, yCenter),
            second.rotate(angleDegrees, xCenter, yCenter)
        )
    }

    fun genParallelLines(
        endPoint: PointF,
        startPoint: PointF,
        space: Float
    ): MutableList<Pair<PointF, PointF>> {
        // Calculate the angle of the line
        val result = mutableListOf<Pair<PointF, PointF>>()
        val deltaX = endPoint.x - startPoint.x
        val deltaY = endPoint.y - startPoint.y
        val angle = Math.atan2(deltaY.toDouble(), deltaX.toDouble())

        val perpendicularDx = space * sin(angle)
        val perpendicularDy = space * cos(angle)

        // Calculate and draw the first parallel line
        val parallelStart1 = PointF(
            startPoint.x - perpendicularDx.toFloat(),
            startPoint.y + perpendicularDy.toFloat()
        )
        val parallelEnd1 = PointF(
            endPoint.x - perpendicularDx.toFloat(),
            endPoint.y + perpendicularDy.toFloat()
        )
        result.add(
            PointF(parallelStart1.x, parallelStart1.y) to PointF(
                parallelEnd1.x,
                parallelEnd1.y
            )
        )

        // Calculate and draw the second parallel line (on the other side)
        val parallelStart2 = PointF(
            startPoint.x + perpendicularDx.toFloat(),
            startPoint.y - perpendicularDy.toFloat()
        )
        val parallelEnd2 = PointF(
            endPoint.x + perpendicularDx.toFloat(),
            endPoint.y - perpendicularDy.toFloat()
        )
        result.add(
            PointF(parallelStart2.x, parallelStart2.y) to PointF(
                parallelEnd2.x,
                parallelEnd2.y
            )
        )
        return result
    }

    private fun findPerpendicularPointOnLine(
        pointLine1: PointF,
        pointLine2: PointF,
        anotherPoint: PointF
    ): PointF {
        // Vector v = pointB - pointA
        val vx = pointLine2.x - pointLine1.x
        val vy = pointLine2.y - pointLine1.y

        // Vector w = pointC - pointA
        val wx = anotherPoint.x - pointLine1.x
        val wy = anotherPoint.y - pointLine1.y

        // Dot products
        val dotWV = wx * vx + wy * vy
        val dotVV = vx * vx + vy * vy

        // Scalar projection factor
        val scalar = dotWV / dotVV

        // Projection vector
        val projX = scalar * vx
        val projY = scalar * vy

        // PointD = pointA + projection vector
        val pointD = PointF(pointLine1.x + projX, pointLine1.y + projY)

        return pointD
    }

    fun pointAtDistanceOnLine(pointA: PointF, pointB: PointF, distance: Float): PointF {
        // Direction vector from A to B
        val dx = pointB.x - pointA.x
        val dy = pointB.y - pointA.y

        // Length of the vector
        val length = sqrt(dx * dx + dy * dy)

        // Unit vector in direction AB
        val ux = dx / length
        val uy = dy / length

        // Scale unit vector by distance and add to pointA
        val cx = pointA.x + ux * distance
        val cy = pointA.y + uy * distance

        return PointF(cx, cy)
    }

    private fun findSpaceBetweenPerpendicularPointToCenterRounded(
        center: PointF,
        pointParallel1: PointF,
        pointParallel2: PointF,
        radiusRounded: Float,
        radiusSweep: Float,
        isOuterRounded: Boolean
    ): Float {
        return if (isOuterRounded) {
            sqrt((radiusSweep - radiusRounded).pow(2) - radiusRounded.pow(2))
        } else {
            sqrt((radiusSweep + radiusRounded).pow(2) - radiusRounded.pow(2))
        }

    }

    private fun findTwoPointSweepRounded(
        spaceBetweenPerpendicularPointToCenterRounded: Float,
        center: PointF,
        innerPoint: PointF,
        centerRounded: PointF,
        radiusSweep: Float
    ): CornerRoundedCoordinate {
        return CornerRoundedCoordinate(
            pointOnLine = pointAtDistanceOnLine(
                center,
                innerPoint,
                spaceBetweenPerpendicularPointToCenterRounded
            ),
            pointOnCurve = pointAtDistanceOnLine(center, centerRounded, radiusSweep)
        )
    }

    fun calculatePositiveAngle(pointA: PointF, pointB: PointF): Float {
        // Calculate the difference in y and x coordinates between the points
        val deltaY = pointB.y - pointA.y
        val deltaX = pointB.x - pointA.x

        // Calculate the angle in radians using atan2
        var angleInRadians = atan2(deltaY, deltaX)

        // Convert the angle to degrees
        var angleInDegrees = Math.toDegrees(angleInRadians.toDouble()).toFloat()

        // Ensure the angle is always positive by adjusting it into the range [0, 360)
        if (angleInDegrees < 0) {
            angleInDegrees += 360f
        }

        return angleInDegrees
    }

    fun calculateAngle(pointA: PointF, pointB: PointF, pointC: PointF): Float {
        // Calculate the distances between points A, B, and C
        val AB = distance(pointA, pointB)
        val BC = distance(pointB, pointC)
        val AC = distance(pointA, pointC)

        // Calculate the cosine of the angle at point B using the law of cosines
        val cosAngle = (AB * AB + BC * BC - AC * AC) / (2 * AB * BC)

        // Ensure the cosine value is between -1 and 1 (due to floating-point precision issues)
        val clampedCosAngle = cosAngle.coerceIn((-1.0).toFloat(), 1.0F)

        // Calculate the angle in radians and then convert it to degrees
        val angleInRadians = acos(clampedCosAngle)
        val angleInDegrees = Math.toDegrees(angleInRadians.toDouble()).toFloat()

        // Ensure the angle is always positive and <= 180 degrees
        return if (angleInDegrees > 180) 360 - angleInDegrees else angleInDegrees
    }

    fun calculateClockwiseAngle(pointA: PointF, pointB: PointF, pointC: PointF): Float {
        // Vectors from B to A and B to C
        val vectorBA = PointF(pointA.x - pointB.x, pointA.y - pointB.y)
        val vectorBC = PointF(pointC.x - pointB.x, pointC.y - pointB.y)

        // Calculate angle between vectors (in radians)
        val angleRadians = atan2(vectorBC.y, vectorBC.x) - atan2(vectorBA.y, vectorBA.x)

        // Convert to degrees and normalize to [0, 360)
        var angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()
        if (angleDegrees < 0) {
            angleDegrees += 360f
        }

        return angleDegrees
    }

    fun distance(p1: PointF, p2: PointF): Float {
        return sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
    }

    fun getRectWrapCircle(centerA: PointF, radiusA: Float): RectF {
        // Calculate the left, top, right, and bottom values for the bounding rectangle
        val left = centerA.x - radiusA
        val top = centerA.y - radiusA
        val right = centerA.x + radiusA
        val bottom = centerA.y + radiusA

        // Return the bounding rectangle as RectF
        return RectF(left, top, right, bottom)
    }

    fun Float.toRadian(): Float {
        return this * PI.toFloat() / 180F
    }

    fun getMidPoint(
        pointA1: PointF,
        pointA2: PointF,
    ): PointF {
        return PointF((pointA1.x + pointA2.x) / 2f, (pointA1.y + pointA2.y) / 2f)
    }

    fun isClockwise(centerPoint: PointF, pointB: PointF, pointC: PointF): Boolean {
        // Calculate angle from center to pointB
        val angleB = atan2(pointB.y - centerPoint.y, pointB.x - centerPoint.x).toDegrees360()

        // Calculate angle from center to pointC
        val angleC = atan2(pointC.y - centerPoint.y, pointC.x - centerPoint.x).toDegrees360()

        // Calculate angle difference from B to C (mod 360 to avoid negatives)
        val angleDiff = (angleC - angleB + 360) % 360

        // If angleDiff is between 0 and 180, it's clockwise (shortest path)
        return angleDiff in 0f..180f
    }

    private fun Float.toDegrees360(): Float {
        val deg = this * 180F / Math.PI.toFloat()
        return ((deg + 360F) % 360F)
    }
}







