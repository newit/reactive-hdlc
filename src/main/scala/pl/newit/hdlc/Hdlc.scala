package pl.newit.hdlc

import akka.stream.scaladsl.Flow
import akka.stream.stage.{Context, PushStage}
import akka.util.ByteString
import pl.newit.hdlc.impl.Framing


object Hdlc {
  case class Settings(
    /** Denotes beginning and the end of the frame. */
    flag: Byte = 0x7e,
    /** Control escape byte used to escaping ``flag`` and ``escape`` itself. */
    escape: Byte = 0x7d,
    /** Bit that will be inverted in escaped byte. */
    escapeBit: Byte = 0x20,
    /** Maximum length of raw frame excluding ``flag``. */
    maximumFrameLength: Int = 256
  )

  def apply(settings: Settings = Settings()) = new Hdlc(settings)
}

class Hdlc(val settings: Hdlc.Settings) {
  val encoder: Flow[ByteString, ByteString, Unit] =
    Flow[ByteString]
      .transform(() ⇒ new Encoder(settings.flag, settings.escape, settings.escapeBit))

  val decoder: Flow[ByteString, ByteString, Unit] =
    Framing.delimiter(settings.flag, settings.maximumFrameLength)
      .drop(1) // everything seen before first flag is junk
      .map { frame ⇒
        val builder = ByteString.newBuilder
        builder.sizeHint(frame.size)

        val it = frame.iterator
        while (it.hasNext) it.next() match {
          case settings.escape if it.hasNext ⇒ builder.putByte((it.next() ^ settings.escapeBit).toByte)
          case settings.escape               ⇒ throw new EofException(frame, "Unexpected end of frame after control escape octet")
          case b                              ⇒ builder.putByte(b)
        }

        builder.result()
      }

  /** INTERNAL API */
  private[hdlc] class Encoder(flag: Byte, escape: Byte, escapeBit: Byte)
    extends PushStage[ByteString, ByteString] {
    private var sof = false
    override def onPush(bytes: ByteString, ctx: Context[ByteString]) = ctx.push {
      val builder = ByteString.newBuilder

      if(!sof) {
        builder.sizeHint(bytes.size + 2)
        builder.putByte(flag)
        sof = true
      } else {
        builder.sizeHint(bytes.size + 1)
      }

      val it = bytes.iterator
      while(it.hasNext) it.next() match {
        case b @ (`flag` | `escape`) ⇒
          builder.putByte(escape)
          builder.putByte((b ^ escapeBit).toByte)
        case b                       ⇒
          builder.putByte(b)
      }

      builder.putByte(flag)
      builder.result()
    }
  }
}
