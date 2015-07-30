package com.challenge.context
import java.io.StringReader
import au.com.bytecode.opencsv.CSVReader
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import com.challenge.utils.DateOperations
import com.challenge.utils.RawSource

object Context {

	def getData(path: String) : RDD[String] = {

		val conf = new SparkConf()
			.setAppName("Playax - Challenge")
			.setMaster("local[4]")

	  val sc = new SparkContext(conf)
	  sc.textFile(path)
	}

	def loadRawSource(data: RDD[String]) : RDD[RawSource] = {
		val rawSource = { data.map { line =>
				val reader = new CSVReader(new StringReader(line), '|');
				reader.readNext()
		}.filter(l => l(0) != "created_at")
		.map { line =>
				val date = DateOperations.getDate(line(0))
				RawSource(date,
					line(1).toLong,
					line(2).toLong,
					date.getDate,
					date.getHours,
					date.getMinutes,
					date.getDay)
			}
		}.repartition(8 * 24).cache()
		rawSource
	}
}

