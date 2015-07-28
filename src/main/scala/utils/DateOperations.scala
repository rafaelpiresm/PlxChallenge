package com.challenge.utils

import java.text.SimpleDateFormat
import java.util.Calendar

object DateOperations {
	def getDate(date: String)  = {
		val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss")    
		formatter.parse(date)
	}	
}

