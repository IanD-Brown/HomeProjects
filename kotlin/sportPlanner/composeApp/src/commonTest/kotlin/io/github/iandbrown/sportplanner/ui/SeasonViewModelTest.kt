package io.github.iandbrown.sportplanner.ui

import androidx.compose.runtime.mutableStateMapOf
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.SeasonCompView
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonDao
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class SeasonViewModelTest : ShouldSpec({
    val seasonDao = mock<SeasonDao>()
    val seasonCompetitionDao = mock<SeasonCompetitionDao>()
    val seasonCompViewDao = mock<SeasonCompViewDao>()

    val competitionState = mock<androidx.compose.runtime.State<List<Competition>>>()

    context("SeasonViewModel.saveCompetitions") {
        should("insert new season competitions") {
            val compId1 = 1.toShort()
            val compId2 = 2.toShort()
            val competitions = listOf(Competition(compId1, "Comp 1", 0),
                Competition(compId2, "Comp 2", 1))
            val startDates = mutableStateMapOf(Pair(compId1, 100), Pair(compId2, 22))
            val endDates = mutableStateMapOf(Pair(compId1, 200), Pair(compId2, 33))

            everySuspend { seasonDao.getSeasonId("Season 1") } returns 123
            every { competitionState.value } returns competitions
            every { seasonDao.get() } returns flowOf(emptyList())
            everySuspend { seasonCompetitionDao.insert(any()) } returns 1L

            val seasonViewModel = SeasonViewModel(seasonDao)
            seasonViewModel.saveCompetitions("Season 1", competitionState, startDates, endDates, seasonCompetitionDao)

            verifySuspend {
                seasonCompetitionDao.insert(SeasonCompetition(123, compId1, 100, 200))
                seasonCompetitionDao.insert(SeasonCompetition(123, compId2, 22, 33))
            }
        }
    }

    context("SeasonCompViewModel.deleteSeason") {
        should("call dao to delete season") {
            val seasonId = 1.toShort()
            every { seasonDao.get() } returns flowOf(emptyList())
            everySuspend { seasonCompViewDao.deleteSeason(seasonId) } returns Unit
            everySuspend { seasonCompViewDao.get() } returns flowOf(emptyList())

            val seasonCompViewModel = SeasonCompViewModel(seasonCompViewDao)
            seasonCompViewModel.deleteSeason(seasonId)

            verifySuspend {
                seasonCompViewDao.deleteSeason(seasonId)
            }
        }
    }

    context("SeasonCompViewModel.getBySeason") {
        should("call dao get by season") {
            val seasonId = 1.toShort()
            val element = SeasonCompView(seasonId, "Season 1",2.toShort(), "Comp 1", 1, 42, 666)
            everySuspend { seasonCompViewDao.getAsList(seasonId) } returns listOf(element)

            val seasonCompViewModel = SeasonCompViewModel(seasonCompViewDao)

            seasonCompViewModel.getBySeason(seasonId).first { it.isNotEmpty() } shouldBe listOf(element)
        }
    }
})
