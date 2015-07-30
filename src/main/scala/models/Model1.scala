package com.challenge.models
/*
Modelo
Esse modelo considera heurísticas acerca do comportamento das audio_source_ids
e track_ids em relção ao tempo.
Utilizamos a implementação do KMeans da própria MLLIB (biblioteca de Machine
Learning do Spark).
Com estes dados não é possível inferir com exatidão o "gênero" ao qual
uma audio_source_id está associada, mas conseguimos posicionar relativamente
próximas, em um espaço dimensionál, audio_sources que tem um comportamento
relativamente próximo, levando em consideração quantidade máxima de plays, média
de plays por hora, etc.
Como o KMeans é um algoritmo muito sensível a escala, aplicamos
uma função de logaritmo para normalizar a escala de alguns valores para que estes
não descaracterizassem o algoritmo.
O modelo em si não performou de forma satisfatória, tendo a sua WSSE muito elevada
(em torno de 969 para 8 clusters).

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
Para 4 clusters, o WSSE foi de 2202
Para 6 clusters, o WSSE foi de  1359
Para 8 clusters, o WSSE foi de  971.
Com isso entendemos, que 8 é um bom número de clusters.
Já que o WSSE ainda é alto e está longe de ser aceitável (quanto menor, melhor),
decidimos assumir 8 clusters apenas, levando em consideração uma maior
capacidade de generalização do modelo, haja vista o risco crescente de
overfitting à medida em que aumentamos o número de clusters.

Desafios/Melhorias
Neste modelo, usamos algumas heurísticas calculadas com base no
timestamp entre as tracks bem como na frequência de plays. Um exemplo de melhoria
que pode ser feito nesse modelo é adicionar basicamente os mesmos contadores, porém
em buckets (por horas/dias/minutos) e buscar entender se há uma melhora significativa
no modelo ou não.

Obs.: WSSE: Within-cluster sum squared error, e é uma medida que pode ser consi-
derada no entendimento da assertividade do clustering.

*/

import com.challenge.context.Context
import java.io.StringReader
import org.apache.spark.mllib.clustering.{KMeans, KMeansModel}
import org.apache.spark.mllib.linalg.Vectors
import scala.math._
import org.apache.spark.rdd.RDD
import com.challenge.utils.AudioSource


object Model1 extends App {

  //loading our data
  val data = Context.getData("/home/rafael/projects/plx/data/detections-redux.csv")
  //val data = sc.textFile("detections.csv")

  //transforming our data in a better layout
  val rawSource = Context.loadRawSource(data)

  //feature engineering
  val audioSource = rawSource.groupBy(s => s.audioSourceId).map {
    case (audioSourceId, rawSources) =>
      AudioSource(audioSourceId,
        scala.math.log(rawSources.toSeq.size.toDouble),
        scala.math.log(rawSources.toSeq.map(c => c.trackId).distinct.size.toDouble),
        rawSources.toSeq.size.toDouble / (60*8*24),
        rawSources.toSeq.map(c => c.trackId).distinct.size.toDouble / (60*8*24),
        rawSources.filter(c => c.weekDay == 0 || c.weekDay == 6).size)
  }.repartition(8 * 24).cache()


  def toVector(audioSources: RDD[AudioSource]) = {
    audioSources.map(s => (s.audioSourceId,Vectors.dense(s.totalPlays,
        s.uniquePlays,
        s.meanPlays,
        s.meanUniquePlays,
        s.totalPlaysAtWeekend))
    )
  }

  val parsedData = toVector(audioSource)

  //model applying
  val iterationCount = 100
  val clusterCount  = 8
  val model = KMeans.train(parsedData.map(d => d._2), clusterCount, iterationCount)
  val cost = model.computeCost(parsedData.map(d => d._2))

  //prediction
  val clusters = model.predict(parsedData.map(d => d._2))
  val clustersWithIds = parsedData.map(d => d._1).zip(clusters)

  //persisting classifications
  clustersWithIds.map {
    case (key,value) => {Array(key,value).mkString(",")}
  }.coalesce(1).saveAsTextFile("result-model1")
}

