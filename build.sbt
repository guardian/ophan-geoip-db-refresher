name := "geoip-db-refresher"

organization := "com.gu"

description:= "Fetching the latest GeoIP database and putting it in S3 for Ophan"

version := "1.0"

scalaVersion := "3.3.0"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8"
)

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.2",
  "com.amazonaws" % "aws-lambda-java-events" % "3.11.2",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.4",
  "org.slf4j" % "log4j-over-slf4j" % "2.0.7", //  log4j-over-slf4j provides `org.apache.log4j.MDC`, which is dynamically loaded by the Lambda runtime
  "ch.qos.logback" % "logback-classic" % "1.4.14",

  "com.lihaoyi" %% "upickle" % "3.1.0",
  "com.google.guava" % "guava" % "32.1.3-jre",
  "org.apache.commons" % "commons-compress" % "1.23.0",
  "commons-io" % "commons-io" % "2.13.0",

) ++ Seq("ssm", "s3", "url-connection-client").map(artifact => "software.amazon.awssdk" % artifact % "2.20.96")

enablePlugins(BuildInfoPlugin)

assembly / assemblyOutputPath  := file(s"target/${name.value}.jar")
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case _ => MergeStrategy.first
}

def env(propName: String): Option[String] = sys.env.get(propName).filter(_.trim.nonEmpty)

buildInfoPackage := "ophan.geoip.extractor"
buildInfoKeys ++= Seq[BuildInfoKey](
  name,
  scalaVersion,
  sbtVersion,

  // copied from https://github.com/guardian/sbt-riffraff-artifact/blob/e6f5e62d8f776b1004f72ed1ea415328fa43ed31/src/main/scala/com/gu/riffraff/artifact/BuildInfo.scala
  BuildInfoKey.sbtbuildinfoConstantEntry("buildNumber", env("GITHUB_RUN_NUMBER")),
  BuildInfoKey.sbtbuildinfoConstantEntry("buildTime", System.currentTimeMillis),
  BuildInfoKey.sbtbuildinfoConstantEntry("gitCommitId", env("GITHUB_SHA")),

  BuildInfoKey.sbtbuildinfoConstantEntry(
    "branch",
    env("GITHUB_HEAD_REF")
      .orElse(env("GITHUB_REF"))
      .orElse(Some("unknown-branch"))
      .get
      .stripPrefix("refs/heads/")),
)