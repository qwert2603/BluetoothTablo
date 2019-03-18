package com.qwert2603.btablo.tablo

data class TabloViewState(
    val game: Game,
    val team1: String,
    val team2: String,
    val minutes: Int,
    val seconds: Int,
    val points1: Int,
    val points2: Int,
    val period: Int,
    val fouls1: Int,
    val fouls2: Int,
    val timeouts1: Int,
    val timeouts2: Int,
    val holdIsTeam2: Boolean,
    val attackSeconds: Int
) {
    fun isPoints1PlusEnabled() = points1 < MAX_POINTS
    fun isPoints1MinusEnabled() = points1 > MIN_POINTS
    fun isPoints2PlusEnabled() = points2 < MAX_POINTS
    fun isPoints2MinusEnabled() = points2 > MIN_POINTS
    fun isPeriodPlusEnabled() = period < MAX_PERIOD
    fun isPeriodMinusEnabled() = period > MIN_PERIOD
    fun isFouls1PlusEnabled() = fouls1 < MAX_FOULS
    fun isFouls1MinusEnabled() = fouls1 > MIN_FOULS
    fun isFouls2PlusEnabled() = fouls2 < MAX_FOULS
    fun isFouls2MinusEnabled() = fouls2 > MIN_FOULS

    companion object {
        const val SECONDS_PER_MINUTE = 60

        const val MAX_SECONDS = 59
        const val MAX_MINUTES = 99

        const val MAX_TOTAL_SECONDS = MAX_MINUTES * SECONDS_PER_MINUTE + MAX_SECONDS

        const val MIN_POINTS = 0
        const val MAX_POINTS = 999

        const val MIN_PERIOD = 0
        const val MAX_PERIOD = 9

        const val MIN_FOULS = 0
        const val MAX_FOULS = 9

        const val MIN_TIMEOUTS = 0
        const val MAX_TIMEOUTS = 3

        fun createDefault(game: Game) = TabloViewState(
            game = game,
            team1 = "Команда1",
            team2 = "Команда2",
            minutes = game.resetSeconds / SECONDS_PER_MINUTE,
            seconds = game.resetSeconds % SECONDS_PER_MINUTE,
            points1 = 0,
            points2 = 0,
            period = 1,
            fouls1 = 0,
            fouls2 = 0,
            timeouts1 = 0,
            timeouts2 = 0,
            holdIsTeam2 = false,
            attackSeconds = 0
        )
    }

    private fun totalSeconds() = minutes * SECONDS_PER_MINUTE + seconds

    fun stepSecond(): TabloViewState {
        val updatedTotalSeconds = totalSeconds()
            .let {
                when {
                    it == game.timeIsOverSeconds -> it
                    game.timeForward -> it + 1
                    else -> it - 1
                }
            }
        return copy(
            minutes = updatedTotalSeconds / SECONDS_PER_MINUTE,
            seconds = updatedTotalSeconds % SECONDS_PER_MINUTE
        )
    }

    fun isTimeOver() = totalSeconds() in listOf(game.timeIsOverSeconds, game.stepsEndTotalSeconds)

    fun decAttackSecond() = copy(attackSeconds = attackSeconds.minus(1).coerceAtLeast(0))
}