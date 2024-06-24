package ophan.geoip.db.refresher

import org.apache.commons.compress.archivers.{ArchiveEntry, ArchiveInputStream}
import org.apache.commons.io.input.CloseShieldInputStream

import java.io.InputStream

class ArchiveInputStreamIterator[E <: ArchiveEntry](archiveInputStream: ArchiveInputStream[E]) extends Iterator[(E, InputStream)] {
  private var latest: E = _

  override def hasNext: Boolean = {
    latest = archiveInputStream.getNextEntry
    latest != null
  }

  override def next(): (E, InputStream) =
    (latest, CloseShieldInputStream.wrap(archiveInputStream))
}