package com.finnvek.active40.domain

import java.util.Locale

enum class WeightUnit { KG, LB }

enum class DistanceUnit { KM, MI }

private const val KG_PER_LB = 0.45359237
private const val METERS_PER_KM = 1000.0
private const val METERS_PER_MILE = 1609.344

fun WeightUnit.fromKg(kg: Double): Double =
    when (this) {
        WeightUnit.KG -> kg
        WeightUnit.LB -> kg / KG_PER_LB
    }

fun WeightUnit.toKg(value: Double): Double =
    when (this) {
        WeightUnit.KG -> value
        WeightUnit.LB -> value * KG_PER_LB
    }

fun DistanceUnit.metersPerUnit(): Double =
    when (this) {
        DistanceUnit.KM -> METERS_PER_KM
        DistanceUnit.MI -> METERS_PER_MILE
    }

fun DistanceUnit.fromMeters(meters: Double): Double = meters / metersPerUnit()

fun DistanceUnit.toMeters(value: Double): Double = value * metersPerUnit()

// Maat, joissa paunat ja mailit ovat arkikäytössä. Iso-Britanniassa juoksumatkat mailein, salipainot kiloin.
private val poundCountries = setOf("US", "LR", "MM")
private val mileCountries = setOf("US", "LR", "MM", "GB")

fun defaultWeightUnit(locale: Locale): WeightUnit = if (locale.country in poundCountries) WeightUnit.LB else WeightUnit.KG

fun defaultDistanceUnit(locale: Locale): DistanceUnit = if (locale.country in mileCountries) DistanceUnit.MI else DistanceUnit.KM

/** Matka valitussa yksikössä ilman yksikkömerkintää, esim. 5.24 tai kokonaismäärinä 18.4. */
fun formatDistance(
    meters: Double,
    unit: DistanceUnit,
    maxDecimals: Int = 2,
): String = formatDecimal(unit.fromMeters(meters), maxDecimals)
