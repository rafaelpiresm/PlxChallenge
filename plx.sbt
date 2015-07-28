name := "PlxChallenge"

scalaVersion := "2.10.4"

version in ThisBuild := "0.1"

val sparkVersion = "1.4.1"
val mllibVersion = "1.4.1"

libraryDependencies ++= Seq(  
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided"
  exclude("org.slf4j", "slf4j-api")
  exclude("org.slf4j", "slf4j-log4j12")
  exclude("org.slf4j", "jul-to-slf4j")
  exclude("org.slf4j", "jcl-over-slf4j")
  exclude("log4j", "log4j"),
  "org.apache.spark"  % "spark-mllib_2.10" % "1.1.0" % "provided",
  "net.sf.opencsv" % "opencsv" % "2.3"
)
