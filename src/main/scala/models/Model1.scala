// package com.challenge.models
// import java.text.SimpleDateFormat
// import java.util.Calendar
// import java.io.StringReader
// import au.com.bytecode.opencsv.CSVReader
// import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
// import org.apache.spark.mllib.linalg.Vectors
// import scala.math._
// import org.apache.spark.rdd.RDD

// //val data = sc.textFile("/home/rafael/projects/playax/data/detections-redux.csv")
// val data = sc.textFile("detections.csv")



// val rawSource = {
// 		data.map { 
// 			line =>
//             	val reader = new CSVReader(new StringReader(line), '|');
//                 reader.readNext()
//         }
//         .filter(l => l(0) != "created_at")
//         .map {line =>
//             	val date = getDate(line(0))                                    
//                 RawSource(
//                     date,
//                     line(1).toLong,  
//                     line(2).toLong,
//                     date.getDate,
//                     date.getHours,
//                     date.getMinutes,
//                     date.getDay
//                 )
                
//         }
// }.repartition(8 * 24).cache()


// val audioSource = rawSource.groupBy(s => s.audioSourceId).map {
// 	case (audioSourceId, rawSources) => 				
// 		AudioSource(scala.math.log(rawSources.toSeq.size.toDouble),
// 					scala.math.log(rawSources.toSeq.map(c => c.trackId).distinct.size.toDouble),
// 					rawSources.toSeq.size.toDouble / (60*8*24),
// 					rawSources.toSeq.map(c => c.trackId).distinct.size.toDouble / (60*8*24))

// 		)		
// }.repartition(8 * 24).cache()



// def toVector(audioSources: RDD[AudioSource]) = {
// 	audioSources.map(s => 
// 		Vectors.dense(s.totalPlays,
// 					  s.uniquePlays,
// 					  s.meanPlays,
// 					  s.meanUniquePlays)
// 	)
// }

// val parsedData = toVector(audioSource)

// val iterationCount = 100
// val clusterCount = 8
// val model = KMeans.train(parsedData, clusterCount, iterationCount)
// val cost = model.computeCost(parsedData)
// println(cost)
