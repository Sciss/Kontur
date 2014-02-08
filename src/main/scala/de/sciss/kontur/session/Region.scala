/*
 *  Region.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur.session

import de.sciss.span.Span

trait RegionTrait[+Repr] extends ResizableStake[Repr] {
  val name: String

  def rename(newName: String): Repr
}

case class Region(span: Span, name: String) extends RegionTrait[Region] {
  def move(delta: Long): Region = copy(span = span.shift(delta))

  def moveStart(delta: Long): Region =
    copy(span = Span(span.start + delta, span.stop))

  def moveStop(delta: Long): Region =
    copy(span = Span(span.start, span.stop + delta))

  def rename(newName: String): Region = copy(name = newName)
}