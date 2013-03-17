package de.sciss.kontur.desktop

object DocumentHandler {
  sealed trait Update[A]
  final case class Added    [A](document: A) extends Update[A]
  final case class Removed  [A](document: A) extends Update[A]
  final case class Activated[A](document: A) extends Update[A]

  type Listener[A] = PartialFunction[Update[A], Unit]
}
trait DocumentHandler {
  import DocumentHandler._

  type Document

  var activeDocument: Option[Document]
  def documents: Iterator[Document]
  def addListener   (listener: Listener[Document]): Listener[Document]
  def removeListener(listener: Listener[Document]): Unit
}