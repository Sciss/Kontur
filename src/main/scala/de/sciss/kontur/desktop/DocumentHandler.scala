package de.sciss.kontur.desktop

trait DocumentHandler {
  type Document

  var activeDocument: Option[Document]
  def documents: Iterator[Document]
}