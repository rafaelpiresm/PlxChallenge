package com.challenge.utils

case class RawSource(createdAt:java.util.Date,
		audioSourceId: Long,
		trackId: Long,
		day: Int,
		hour: Int,
		min: Int,
		weekDay: Int)

case class AudioSource(audioSourceId: Long,
		totalPlays: Double,
		uniquePlays: Double,
		meanPlays: Double,
		meanUniquePlays: Double,
		totalPlaysAtWeekend: Double)