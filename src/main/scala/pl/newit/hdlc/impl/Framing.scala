package pl.newit.hdlc.impl

import akka.stream.scaladsl.Flow
import akka.stream.stage.{Context, PushPullStage, SyncDirective, TerminationDirective}
import akka.util.ByteString
import pl.newit.hdlc.FramingException

/** INTERNAL API */
private[hdlc] object Framing {
  /**
   * Creates a Flow that handles decoding a stream of unstructured byte chunks into a stream of frames where the
   * incoming chunk stream uses a specific byte to mark frame boundaries.
   *
   * The decoded frames will not include the separator.
   *
   * If there are buffered bytes (an incomplete frame) when the input stream finishes and ''allowTruncation'' is set to
   * false then this Flow will fail the stream reporting a truncated frame.
   *
   * @param delimiter The byte to be treated as the end of the frame.
   * @param allowTruncation If turned on, then when the last frame being decoded contains no valid delimiter this Flow
   *                        fails the stream instead of returning a truncated frame.
   * @param maximumFrameLength The maximum length of allowed frames while decoding. If the maximum length is
   *                           exceeded this Flow will fail the stream.
   * @return
   */
  def delimiter(delimiter: Byte, maximumFrameLength: Int, allowTruncation: Boolean = false): Flow[ByteString, ByteString, Unit] =
    Flow[ByteString].transform(() ⇒ new SingleByteDelimiterFramingStage(delimiter, maximumFrameLength, allowTruncation))
      .named("singleByteDelimiterFraming")


  private class SingleByteDelimiterFramingStage(separator: Byte, maximumLineBytes: Int, allowTruncation: Boolean)
    extends PushPullStage[ByteString, ByteString] {
    private var buffer = ByteString.empty
    private var nextPossibleMatch = 0
    private var finishing = false

    override def onPush(chunk: ByteString, ctx: Context[ByteString]): SyncDirective = {
      buffer ++= chunk
      doParse(ctx)
    }

    override def onPull(ctx: Context[ByteString]): SyncDirective = {
      doParse(ctx)
    }

    override def onUpstreamFinish(ctx: Context[ByteString]): TerminationDirective = {
      if (buffer.nonEmpty) ctx.absorbTermination()
      else ctx.finish()
    }

    private def tryPull(ctx: Context[ByteString]): SyncDirective = {
      if (ctx.isFinishing) {
        if (allowTruncation) ctx.pushAndFinish(buffer)
        else
          ctx.fail(new FramingException(
            "Stream finished but there was a truncated final frame in the buffer"))
      } else ctx.pull()
    }

    private def doParse(ctx: Context[ByteString]): SyncDirective = {
      val possibleMatchPos = buffer.indexOf(separator, from = nextPossibleMatch)
      if (possibleMatchPos > maximumLineBytes)
        ctx.fail(new FramingException(s"Read ${buffer.size} bytes " +
          s"which is more than $maximumLineBytes without seeing a line terminator"))
      else {
        if (possibleMatchPos == -1) {
          // No matching character, we need to accumulate more bytes into the buffer
          nextPossibleMatch = buffer.size
          tryPull(ctx)
        } else {
          // Found a match
          val parsedFrame = buffer.slice(0, possibleMatchPos).compact
          buffer = buffer.drop(possibleMatchPos + 1)
          nextPossibleMatch = 0
          if (ctx.isFinishing && buffer.isEmpty) ctx.pushAndFinish(parsedFrame)
          else ctx.push(parsedFrame)
        }
      }
    }

    override def postStop(): Unit = buffer = null
  }
}
