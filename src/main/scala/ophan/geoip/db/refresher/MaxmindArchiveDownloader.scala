package ophan.geoip.db.refresher

import com.google.common.hash.{HashCode, Hashing}
import com.google.common.io.Files
import ophan.geoip.db.refresher.logging.Logging

import java.io.File
import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version.HTTP_2
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.nio.file.Path
import java.nio.file.StandardOpenOption.{CREATE, WRITE}
import java.time.Duration.ofSeconds
import scala.util.{Failure, Success, Try}

object MaxmindArchiveDownloader extends Logging {

  val client: HttpClient = HttpClient.newBuilder.version(HTTP_2).followRedirects(Redirect.NORMAL)
    .connectTimeout(ofSeconds(20)).build

  def download(maxmindArchive: MaxmindDatabaseEdition): Try[File] = for {
    expectedHash <- fetchExpectedHash(maxmindArchive)
    downloadedFile <- fetchArchive(maxmindArchive)
    _ <- checkHash(downloadedFile, expectedHash)
  } yield downloadedFile

  private def checkHash(downloadedFile: File, expectedHash: HashCode): Try[File] = {
    val hashOfDownloadedArchive = Files.asByteSource(downloadedFile).hash(Hashing.sha256())

    logger.info(Map(
        "downloadedArchive.expectedHash" -> expectedHash.toString,
        "downloadedArchive.hash" -> hashOfDownloadedArchive.toString
      ), s"$hashOfDownloadedArchive")
    if (hashOfDownloadedArchive == expectedHash) Success(downloadedFile) else Failure(new RuntimeException())
  }

  private def fetchExpectedHash(maxmindArchive: MaxmindDatabaseEdition): Try[HashCode] = {
    val response = client.send(request(maxmindArchive.hashUrl), BodyHandlers.ofString)
    if (response.statusCode() == 200) Success(HashCode.fromString(response.body.split(' ').head)) else Failure(new RuntimeException())
  }

  private def fetchArchive(maxmindArchive: MaxmindDatabaseEdition): Try[File] = {
    val response = client.send(request(maxmindArchive.archiveUrl), BodyHandlers.ofFileDownload(Path.of("/tmp"), CREATE, WRITE))
    val downloadedArchiveFilePath = response.body()
    logger.info(Map(
        "downloadedArchive.path" -> downloadedArchiveFilePath.toString
      ), s"$downloadedArchiveFilePath")
    Success(downloadedArchiveFilePath.toFile)
  }

  def request(uri: URI) = HttpRequest.newBuilder(uri).GET().build()

}
