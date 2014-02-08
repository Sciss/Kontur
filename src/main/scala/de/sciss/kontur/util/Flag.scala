/*
 *  Flag.scala
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

package de.sciss.kontur.util

object Flag {
  def False() = new Flag(false)
  def True()  = new Flag(true)
  def apply(init: Boolean) = new Flag(init)
//  implicit def value(f: Flag) = f.value
}
final class Flag private(var value: Boolean) {
  override def toString = s"Flag($value)"
  def apply() = value
  def update(value: Boolean): Unit = this.value = value
}