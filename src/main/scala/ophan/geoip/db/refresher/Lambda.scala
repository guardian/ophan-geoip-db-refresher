package ophan.geoip.db.refresher

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import ophan.geoip.db.refresher.logging.Logging
import software.amazon.awssdk.services.s3.model.PutObjectRequest

object Lambda extends Logging {

  /*
   * Logic handler
   */
  def go(): Unit = {
    retrieveAndStore(MaxmindDatabaseEdition.GeoIP2City)
  }

  private def retrieveAndStore(maxmindDatabaseEdition: MaxmindDatabaseEdition) = {
    for {
      downloadedArchiveFile <- MaxmindArchiveDownloader.download(maxmindDatabaseEdition)
    } {
      maxmindDatabaseEdition.usingDatabaseStreamFrom(downloadedArchiveFile) { streamOfKnownSize =>
        val s3Key = s"geoip/test-${maxmindDatabaseEdition.databaseFileName}"
        val objectRequest = PutObjectRequest.builder.bucket("ophan-dist").key(s3Key).build

        logger.info(Map(
            "databaseFile.size" -> streamOfKnownSize.size,
            "databaseFile.s3.destination" -> s3Key
          ), s"Starting upload of ${streamOfKnownSize.size} bytes to S3 ($objectRequest)")
        val putObjectResponse = AWS.S3.putObject(objectRequest, streamOfKnownSize.asAwsSdkRequestBody())
        logger.info(Map(
            "databaseFile.s3.versionId" -> putObjectResponse.versionId
          ), s"Upload complete!")
      }
    }
  }

  /*
   * Lambda's entry point
   */
  def handler(lambdaInput: ScheduledEvent, context: Context): Unit = {
    go()
  }

}

