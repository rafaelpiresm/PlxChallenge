package com.challenge.models

/*
Modelo
Esse modelo considera todas as track_ids em um vetor único para cada 
audio_source_id.
Utilizamos a implementação do KMeans da própria MLLIB (biblioteca de Machine 
Learning do Spark). 
Com estes dados não é possível inferir com exatidão o "gênero" ao qual 
uma audio_source_id está associada, mas conseguimos posicionar relativamente 
próximas, em um espaço dimensionál, audio_sources que tem em comum as 
mesmas track_ids.
Como o KMeans é um algoritmo muito sensível a escala, primeiramente aplicamos 
uma função de logaritmo para normalizar a escala cujos IDs das tracks se 
apresentam. Infelizmente a função de logaritmo não foi suficiente, então 
recorremos a uma função da própria MLLIB do Spark para fazer essa transformação,
a qual aparesentou resultados bem melhores (o WSSE do algoritmo caiu da casa de 
milhares para centenas).

Processo 
Para desenvolver o modelo e testar o approach, inicialmente, reduzimos o 
dataset a um volume menor, mas ainda assim significante.
Após algumas implementações, montamos um cluster de Spark na AWS (8 EC2 do tipo 
m3.2xlarge - 8CPUs + 30GB de RAM + 2 SSD x 80GB)
O resultado foi computado em 7minutos.
Deve-se considerar:
1 - esta máquina é otimizada para memória, mas o ideal seria termos trabalhado
	com máquinas otimizadas para computação, porém estas estavam fora da zona 
	de disponibilidade da conta utilizada para testes;
2 - não houve muita preocupação com tunning de código. Apenas foram feitas 
	algumas repartições levando em consideração o tamanho do cluster, junstamente
	com a persistência de alguns dados que seriam reutilizados em memória

Resultados
Computados os resultados dos erros, tivemos os seguintes valores:
Para 4 clusters, o WSSE foi de 1024
Para 6 clusters, o WSSE foi de  759
Para 8 clusters, o WSSE foi de  602.
Com isso entendemos, que 8 é um bom número de clusters. 
Já que o WSSE ainda é alto e está longe de ser aceitável (quanto menor, melhor), 
decidimos assumir 8 clusters apenas, levando em consideração uma maior 
capacidade de generalização do modelo, haja vista o risco crescente de 
overfitting à medida em que aumentamos o número de clusters.

Desafios
Neste modelo, usamos basicamente uma coluna contendo todas as tracks tocadas por
uma determinada audio_source. Por exemplo, algumas audio_sources tocaram 
2000 tracks (incluindo bis), enquanto outras tocaram 300. 
Para inputarmos corretamente os valores no algoritmo, precisamos fixar o número 
de colunas de acordo com a audio_source que mais tocou. Nesta versão de 
algoritmo, apenas completamos os vetores que não atingiram o máximo com "0", 
em sua respectiva cauda. 

Melhorias
Tentar encontrar esses "gaps" de forma mais inteligente, usando o timestamp 
fornecido talvez apresente resultados melhores, já que os "0" não estarão sempre 
ao final do vetor, mas sim parecerão intercalados pelo data set.
*/

import com.challenge.utils.DateOperations
import java.io.StringReader
import au.com.bytecode.opencsv.CSVReader
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors
import scala.math._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.feature.Normalizer
import com.challenge.utils.RawSource
import com.challenge.utils.AudioSource

object Model2 extends App {	
	
  val conf = new SparkConf()
		.setAppName("Playax - Challenge")
		.setMaster("local[4]")      

  val sc = new SparkContext(conf) 	  

  val data = sc.textFile("/home/rafael/projects/plx/data/detections.csv")

  //loading our data in a better layout
  val rawSource = {
		data.map { line =>
					val reader = new CSVReader(new StringReader(line), '|');
					reader.readNext()
		}
		.filter(l => l(0) != "created_at")
		.map { line =>
				val date = DateOperations.getDate(line(0))
				RawSource(
				date,
				line(1).toLong,  
				line(2).toLong,
				date.getDate,
				date.getHours,
				date.getMinutes,
				date.getDay
				)				
		}
	}.repartition(8 * 24).cache()

	//feature engineering
	val rawSourceGrouped = rawSource
		.groupBy(s => s.audioSourceId).repartition(8 * 24).cache()		
	val max = rawSourceGrouped.map(s => s._2.toSeq.size).max -1 
	val parsedData = rawSourceGrouped.map(s => s._2).map {
			r => {									
				val array = r.toSeq
					.map(s => (scala.math.log(s.trackId.toDouble))).toArray 
				val compl = (array.size to max).toArray
				val arrayCompl = array ++ compl.map(v => 0.toDouble)			
				(r.head.audioSourceId, arrayCompl)
			}
		}.repartition(8 * 24).cache()

	//model applying
	val iterationCount = 1000
	val clusterCount = 8
	val normalizer = new Normalizer()
	val vectors = parsedData.map(d => Vectors.dense(d._2))
	val normalizedVectors = normalizer.transform(vectors)
	val model = KMeans.train(normalizedVectors,clusterCount, iterationCount)
	val cost = model.computeCost(normalizedVectors)
	
	println("=============================================================")
	println("Error: " + cost)		
	println("=============================================================")

	//prediction
	val clusters = model.predict(normalizedVectors)
	val clustersWithIds = parsedData.map(d => d._1).zip(clusters)

	//persisting classifications
	clustersWithIds.map {
		case (key,value) => {Array(key,value).mkString(",")}
	}.coalesce(1).saveAsTextFile("result")	
}




