import com.gu.riffraff.artifact.BuildInfo

name := "geoip-db-refresher"

organization := "com.gu"

description:= "Fetching the latest GeoIP database and putting it in S3 for Ophan"

version := "1.0"

scalaVersion := "3.1.3"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8"
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.2",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.36", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime
  "ch.qos.logback" % "logback-classic" % "1.2.11",

  "com.lihaoyi" %% "upickle" % "1.6.0",
  "com.google.guava" % "guava" % "31.1-jre",
  "org.apache.commons" % "commons-compress" % "1.21",
  "commons-io" % "commons-io" % "2.11.0",

) ++ Seq("ssm", "s3", "url-connection-client").map(artifact => "software.amazon.awssdk" % artifact % "2.17.259")

enablePlugins(RiffRaffArtifact, BuildInfoPlugin)

assemblyJarName := s"${name.value}.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffArtifactResources += (file("cfn.yaml"), s"cloudformation/cfn.yaml")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}


buildInfoPackage := "ophan.geoip.extractor"
buildInfoKeys := {
  lazy val buildInfo = BuildInfo(baseDirectory.value)
  Seq[BuildInfoKey](
    "buildNumber" -> buildInfo.buildIdentifier,
    "gitCommitId" -> buildInfo.revision,
    "buildTime" -> System.currentTimeMillis
  )
}
