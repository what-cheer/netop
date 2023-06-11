package whatcheer.utils

import ch.qos.logback.core.ConsoleAppender
import java.io.OutputStream
import net.liftweb.util.ThreadGlobal

/**
* Set this ThreadGlobal with a doWith so that you can get per-thread
* log information
*/
object Logstream extends ThreadGlobal[OutputStream]

class PerThreadLogger[E] extends ConsoleAppender[E] {
  private class ProxyOutputStream(inner: OutputStream) extends OutputStream {
    private def choose: OutputStream = Logstream.box openOr inner

    def write(i: Int): Unit = {
      choose.write(i)
    }

    override def write(bytes: Array[Byte]): Unit = {
      choose.write(bytes)
    }

    override def write(bytes: Array[Byte], i: Int, i1: Int): Unit = {
      choose.write(bytes, i, i1)
    }

    override def flush(): Unit = {choose.flush()}

    override def close(): Unit = {choose.close()}
  }
}

