package io.github.iandbrown.sportplanner.ui

import androidx.compose.runtime.mutableStateMapOf
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.github.iandbrown.sportplanner.database.Competition
import io.github.iandbrown.sportplanner.database.SeasonCompViewDao
import io.github.iandbrown.sportplanner.database.SeasonCompetition
import io.github.iandbrown.sportplanner.database.SeasonCompetitionDao
import io.github.iandbrown.sportplanner.database.SeasonDao
import io.kotest.core.spec.style.ShouldSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonViewModelTest : ShouldSpec({
    val seasonDao = mock<SeasonDao>()
    val seasonCompetitionDao = mock<SeasonCompetitionDao>()

    val testDispatcher = StandardTestDispatcher()

    beforeTest {
        Dispatchers.setMain(testDispatcher)
    }

    afterTest {
        Dispatchers.resetMain()
    }

    context("SeasonViewModel.saveCompetitions") {
        should("insert new season competitions") {
            runTest {
                val compId1 = 1.toShort()
                val compId2 = 2.toShort()
                val competition = listOf(
                    Competition(compId1, "Comp 1", 0),
                    Competition(compId2, "Comp 2", 1)
                )
                val startDates = mutableStateMapOf(Pair(compId1, 100), Pair(compId2, 22))
                val endDates = mutableStateMapOf(Pair(compId1, 200), Pair(compId2, 33))

                everySuspend { seasonDao.getSeasonId("Season 1") } returns 123.toShort()
                everySuspend { seasonCompetitionDao.insert(any()) } returns 1L

                val seasonViewModel = SeasonViewModel(seasonDao)
                seasonViewModel.save(null, "Season 1", competition, startDates, endDates, seasonCompetitionDao); advanceUntilIdle()

                verifySuspend {
                    seasonCompetitionDao.insert(SeasonCompetition(123.toShort(), compId1, 100, 200))
                    seasonCompetitionDao.insert(SeasonCompetition(123.toShort(), compId2, 22, 33))
                }
            }
        }
    }

    context("SeasonCompViewModel.deleteSeason") {
        should("call dao to delete season") {
            runTest {
                val dao = mock<SeasonCompViewDao>()

                everySuspend { dao.deleteSeason(any()) } returns Unit

                val seasonId = 1.toShort()
                val viewModel = SeasonCompViewModel(dao)

                viewModel.deleteSeason(seasonId)

                advanceUntilIdle()

                verifySuspend {
                    dao.deleteSeason(seasonId)
                }
            }
        }
    }

})
