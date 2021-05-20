package ophan.geoip.db.refresher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import ophan.geoip.db.refresher.logging.Logging
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import scala.util.Failure
import scala.util.Success

object Lambda extends Logging {

  /*
   * Logic handler
   */
  def go(): Unit = {
    retrieveAndStore(MaxmindDatabaseEdition.GeoIP2City)
  }

  // See https://docs.aws.amazon.com/AmazonS3/latest/userguide/finding-canonical-user-id.html
  val DeployToolsAWSAccountCanonicalUserId = "4545b54bd17af766e5e14aa12fd41bade300cf170dc6f5c4cd09240d36484cf1"

  private def retrieveAndStore(maxmindDatabaseEdition: MaxmindDatabaseEdition) = {
    (for {
      downloadedArchiveFile <- MaxmindArchiveDownloader.download(maxmindDatabaseEdition)
      _ <- maxmindDatabaseEdition.usingDatabaseStreamFrom(downloadedArchiveFile) { streamOfKnownSize =>
        val s3Key = s"geoip/${maxmindDatabaseEdition.databaseFileName}"
        val objectRequest = PutObjectRequest.builder.bucket("ophan-dist").key(s3Key)
          .grantRead(s"id=$DeployToolsAWSAccountCanonicalUserId") // so TeamCity can read file for CI tests
          .build

        logger.info(Map(
          "databaseFile.size" -> streamOfKnownSize.size,
          "databaseFile.s3.destination" -> s3Key
        ), s"Starting upload of ${streamOfKnownSize.size} bytes to S3 ($objectRequest)")
        val putObjectResponse = AWS.S3.putObject(objectRequest, streamOfKnownSize.asAwsSdkRequestBody())
        logger.info(Map(
          "databaseFile.s3.versionId" -> putObjectResponse.versionId
        ), s"Upload complete!")
      }
    } yield ()) match {
      case Failure(err) => logger.error(s"Failure to retrieve url", err)
      case _:Success[_] =>
    }
  }

  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: ScheduledEvent, context: Context): Unit = {
    go()
  }

}
