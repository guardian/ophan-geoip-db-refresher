package ophan.geoip.db.refresher

import ophan.geoip.db.refresher.MaxmindDatabaseEdition.uriFor
import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveStreamFactory, ArchiveInputStream}
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
    implicit def archiveInputStreamReleasable[E <: ArchiveEntry]: Using.Releasable[ArchiveInputStream[E]] =
      (resource: ArchiveInputStream[E]) => resource.close()

    Using(new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(
      new CompressorStreamFactory().createCompressorInputStream(new BufferedInputStream(
        new FileInputStream(archiveFile)
      ))
    )).asInstanceOf[ArchiveInputStream[ArchiveEntry]]) { archiveInputStream =>
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
  val GeoIP2Country = MaxmindDatabaseEdition("GeoIP2-Country", "tar.gz", "mmdb") // smaller file useful for test runs

  def uriFor(editionId: String, suffix: String): URI =
    new URI(s"https://download.maxmind.com/app/geoip_download?edition_id=$editionId&license_key=$licenceKey&suffix=$suffix")
}
