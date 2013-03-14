package de.sciss.kontur.util

import language.implicitConversions

object Flag {
  def apply(init: Boolean) = new Flag(init)
  implicit def value(f: Flag) = f.value
}
final class Flag private(var value: Boolean) {
  override def toString = s"Flag($value)"
}