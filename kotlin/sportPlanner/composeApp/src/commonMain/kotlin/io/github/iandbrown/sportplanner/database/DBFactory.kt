package io.github.iandbrown.sportplanner.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

fun populateDbOnCreate(connection: SQLiteConnection) {
    for (name in listOf("DONCASTER", "ROTHERHAM", "BARNSLEY", "LEEDS", "EAST RIDING", "SELBY", "HUDDERSFIELD")) {
        connection.execSQL("INSERT INTO Associations (name) VALUES ('$name')")
    }

    for (name in listOf("u11", "u12", "u13", "u14", "u15")) {
        connection.execSQL("INSERT INTO TeamCategories (name, matchDay) VALUES ('$name Boys', 5)")
        connection.execSQL("INSERT INTO TeamCategories (name, matchDay) VALUES ('$name Girls', 6)")
    }

    connection.execSQL("INSERT INTO Competitions (name, type) VALUES ('League', 0)")
    connection.execSQL("INSERT INTO Competitions (name, type) VALUES ('Cup', 1)")

    var date = 1
    var date2 = 300
    for (name in listOf("20-21", "21-22", "22-23", "24-25", "25-26")) {
        connection.execSQL("INSERT INTO Seasons (name) VALUES ('$name')")
        connection.execSQL("INSERT INTO SeasonTeams (seasonId, competitionId, associationId, teamCategoryId, count) " +
                "SELECT $date, 1, a.id, tc.id, 1 FROM Associations a, TeamCategories tc ")
        connection.execSQL("INSERT INTO SeasonTeamCategories (seasonId, competitionId, teamCategoryId, games, locked) " +
                "SELECT $date, 1, tc.id, 1, 0 FROM TeamCategories tc")
        connection.execSQL("INSERT INTO SeasonCompetitions (seasonId, competitionId, startDate, endDate) VALUES ($date, 1, $date, $date2)")
        ++date
        ++date2
    }

}
