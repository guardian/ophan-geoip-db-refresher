package ophan.geoip.db.refresher

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream}
import org.apache.commons.io.input.CloseShieldInputStream

import java.io.InputStream

class ArchiveInputStreamIterator(archiveInputStream: ArchiveInputStream) extends Iterator[(ArchiveEntry, InputStream)] {
  private var latest: ArchiveEntry = _

  override def hasNext: Boolean = {
    latest = archiveInputStream.getNextEntry

    latest != null
  }

  override def next(): (ArchiveEntry, InputStream) =
    (latest, CloseShieldInputStream.wrap(archiveInputStream))
}