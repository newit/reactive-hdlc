package pl.newit.hdlc

import akka.util.ByteString

class HdlcException(msg: String) extends RuntimeException(msg)

class FramingException(msg: String) extends HdlcException(msg)
class EofException(frame: ByteString, msg: String) extends HdlcException(msg)
