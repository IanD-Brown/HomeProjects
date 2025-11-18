package io.github.iandbrown.sportplanner

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform