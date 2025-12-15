kotlin multiplatform application intended to help with planning a season (target being football and the district schools associations)

Uses a number of 3rd party libraries

To create an executable for windows run
* Room and sqlite for data storage
* koin for dependency injection
* kotest for unit testing
* kover for code coverage
* theme-material for dark/light theme setting
* compose-data-table for a data table view
* file-kit for file picker and write access

`gradlew.bat createDistributable`

The output will be in the sub-directory '.\composeApp\build\compose\binaries\main\app'