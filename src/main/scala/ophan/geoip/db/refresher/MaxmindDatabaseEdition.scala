package ophan.geoip.db.refresher

import ophan.geoip.db.refresher.MaxmindDatabaseEdition.uriFor
import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveStreamFactory}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import software.amazon.awssdk.core.sync.RequestBody

import java.io.{BufferedInputStream, File, FileInputStream, InputStream}
import java.net.URI
import java.nio.file.Paths
import scala.util.Using

case class StreamOfKnownSize(stream: InputStream, size: Long) {
  def asAwsSdkRequestBody() = RequestBody.fromInputStream(stream, size)
}

case class MaxmindDatabaseEdition(editionId: String, archiveSuffix: String, databaseFileSuffix: String) {

  private val archiveHashSuffix: String = archiveSuffix + ".sha256"

  val archiveUrl = uriFor(editionId, archiveSuffix)
  val hashUrl = uriFor(editionId, archiveHashSuffix)

  val databaseFileName = s"$editionId.$databaseFileSuffix"

  def usingDatabaseStreamFrom(archiveFile: File)(process: StreamOfKnownSize => Unit) = {
    Using(new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(
      new CompressorStreamFactory().createCompressorInputStream(new BufferedInputStream(
        new FileInputStream(archiveFile)
      ))
    ))) { archiveInputStream =>
      for {
        (archiveEntry: ArchiveEntry, stream) <- new ArchiveInputStreamIterator(archiveInputStream)
        if Paths.get(archiveEntry.getName).endsWith(databaseFileName)
      } {
        process(StreamOfKnownSize(stream, archiveEntry.getSize))
      }
    }
  }
}

object MaxmindDatabaseEdition {

  val licenceKey: String = AWS.SSM.getParameter(_.withDecryption(true).name("/Ophan/GeoIP")).parameter.value

  val GeoIP2City = MaxmindDatabaseEdition("GeoIP2-City", "tar.gz", "mmdb")

  def uriFor(editionId: String, suffix: String): URI =
    new URI(s"https://download.maxmind.com/app/geoip_download?edition_id=$editionId&license_key=$licenceKey&suffix=$suffix")
}