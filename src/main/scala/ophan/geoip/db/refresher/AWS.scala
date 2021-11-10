package ophan.geoip.db.refresher

import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder
import software.amazon.awssdk.http.{ExecutableHttpRequest, HttpExecuteRequest, SdkHttpClient}
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.Region.EU_WEST_1
import software.amazon.awssdk.services.s3.{S3Client, S3ClientBuilder}
import software.amazon.awssdk.services.ssm.{SsmClient, SsmClientBuilder}

import java.net.HttpURLConnection

object AWS {
  val region: Region = EU_WEST_1

  def credentialsForDevAndProd(devProfile: String, prodCreds: AwsCredentialsProvider): AwsCredentialsProviderChain =
    AwsCredentialsProviderChain.of(prodCreds, ProfileCredentialsProvider.builder().profileName(devProfile).build())

  lazy val credentials: AwsCredentialsProvider =
    credentialsForDevAndProd("ophan", EnvironmentVariableCredentialsProvider.create())

  def build[T, B <: AwsClientBuilder[B, T]](builder: B): T =
    builder.credentialsProvider(credentials).region(region).build()

  private val sdkHttpClient: SdkHttpClient = new SdkHttpClient {
    val wrapped: SdkHttpClient = UrlConnectionHttpClient.builder().build()

    override def prepareRequest(request: HttpExecuteRequest): ExecutableHttpRequest = {
      val exHttpReq = wrapped.prepareRequest(request)

      val f = exHttpReq.getClass.getDeclaredField("connection")
      f.setAccessible(true)
      val connection = f.get(exHttpReq).asInstanceOf[HttpURLConnection]
      val STREAMING_OUTPUT_THRESHOLD = 1 * 1024 * 1024

      request.httpRequest.firstMatchingHeader("Content-Length").map(java.lang.Long.parseLong)
        .filter(len => len > STREAMING_OUTPUT_THRESHOLD)
        .ifPresent(connection.setFixedLengthStreamingMode)

      exHttpReq
    }

    override def close(): Unit = wrapped.close()
  }
  val SSM = build[SsmClient, SsmClientBuilder](SsmClient.builder().httpClient(sdkHttpClient))
  val S3 = build[S3Client, S3ClientBuilder](S3Client.builder().httpClient(sdkHttpClient))
}
