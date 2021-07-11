package de.rki.coronawarnapp.statistics.local.source

import dagger.Reusable
import de.rki.coronawarnapp.datadonation.analytics.common.Districts
import de.rki.coronawarnapp.server.protocols.internal.stats.KeyFigureCardOuterClass
import de.rki.coronawarnapp.server.protocols.internal.stats.LocalStatisticsOuterClass
import de.rki.coronawarnapp.statistics.LocalIncidenceStats
import de.rki.coronawarnapp.statistics.LocalStatisticsData
import de.rki.coronawarnapp.util.ui.toLazyString
import org.joda.time.Instant
import timber.log.Timber
import javax.inject.Inject

@Reusable
class LocalStatisticsParser @Inject constructor(
    private val districtSource: Districts
) {
    private val districts by lazy {
        districtSource.loadDistricts()
    }

    private fun getNameForId(districtId: Int) =
        districts.first { it.districtId == districtId }.districtName.toLazyString()

    fun parse(rawData: ByteArray): LocalStatisticsData {
        val parsed = LocalStatisticsOuterClass.LocalStatistics.parseFrom(rawData)

        val mappedAdministrativeUnit = parsed.administrativeUnitDataList.mapNotNull { rawState ->
            try {
                val updatedAt = Instant.ofEpochSecond(rawState.updatedAt)
                val administrativeUnitIncidenceKeyFigure = rawState.sevenDayIncidence.toKeyFigure()

                val leftPaddedShortId = rawState.administrativeUnitShortId
                    .toString()
                    .padStart(5, '0')

                val districtId = "110${leftPaddedShortId}".toInt()

                val districtName = getNameForId(districtId)

                LocalIncidenceStats(
                    updatedAt = updatedAt,
                    keyFigures = listOf(administrativeUnitIncidenceKeyFigure),
                    districtId = districtId,
                    districtName = districtName,
                ).also {
                    Timber.tag(TAG).v("Parsed %s", it.toString().replace("\n", ", "))
                    it.requireValidity()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("Failed to parse raw federal state: %s", rawState)
                null
            }
        }

        return LocalStatisticsData(items = mappedAdministrativeUnit).also {
            Timber.tag(TAG).d("Parsed local statistics data, %d cards.", it.items.size)
        }
    }

    private fun LocalStatisticsOuterClass.SevenDayIncidenceData.toKeyFigure(): KeyFigureCardOuterClass.KeyFigure =
        KeyFigureCardOuterClass.KeyFigure.newBuilder()
            .setRank(KeyFigureCardOuterClass.KeyFigure.Rank.PRIMARY)
            .setValue(value)
            .setDecimals(0)
            .setTrend(trend)
            .setTrendSemantic(KeyFigureCardOuterClass.KeyFigure.TrendSemantic.UNSPECIFIED_TREND_SEMANTIC)
            .build()

    companion object {
        const val TAG = "StatisticsParser"
    }
}
