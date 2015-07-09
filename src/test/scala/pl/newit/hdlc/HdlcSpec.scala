package pl.newit.hdlc

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import org.scalacheck.{Arbitrary, Properties}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object HdlcSpec extends Properties("Hdlc") {
  implicit val system = ActorSystem(name)
  implicit val mat = ActorMaterializer()

  implicit val byteString = Arbitrary(
    for (bytes ← arbitrary[Array[Byte]])
      yield ByteString(bytes)
  )

  def result[T](f: Future[T]): T = Await.result(f, 1.second)
  def toVector[T]: Sink[T, Future[Vector[T]]] = Sink.fold(Vector.empty[T])((acc, m) ⇒ acc :+ m)

  def encode(bytes: ByteString): (Hdlc, ByteString) = {
    val hdlc = Hdlc()

    // 1 in ⇒ 1 out
    val Vector(encoded) = result(
      Source.single(bytes)
        .via(hdlc.encoder)
        .runWith(toVector)
    )

    (hdlc, encoded)
  }

  property("encode: size") = forAll { message: ByteString ⇒
    val (hdlc, encoded) = encode(message)
    val specials = message.count(b ⇒ b == hdlc.settings.flag || b == hdlc.settings.escape)

    // sof + payload(+ escaping) + eof
    encoded.size == (message.size + specials + 2)
  }

  property("encode: flag") = forAll { message: ByteString ⇒
    val (hdlc, encoded) = encode(message)

    encoded.head == hdlc.settings.flag && // flag at the beginning
      encoded.last == hdlc.settings.flag && // flag at the end
      encoded.slice(1, encoded.size - 1)
        .forall(_ != hdlc.settings.flag) // flag nowhere else
  }

  property("encode: escape") = forAll { message: ByteString ⇒
    val (hdlc, encoded) = encode(message)
    val specials = message.count(b ⇒ b == hdlc.settings.flag || b == hdlc.settings.escape)

    encoded.count(_ == hdlc.settings.escape) == specials && // escape for every special
      encoded.zipWithIndex.forall { // escape precedes special
        case (hdlc.settings.escape, i) ⇒
          val decoded = encoded(i + 1) ^ hdlc.settings.escapeBit
          decoded == hdlc.settings.flag || decoded == hdlc.settings.escape
        case _                         ⇒ true
      }
  }

  property("encode → decode") = forAll((messages: Vector[ByteString]) ⇒
    result(
      Source(messages)
        .via(Hdlc().encoder)
        .via(Hdlc().decoder)
        .runWith(toVector)
    ) == messages
  )
}