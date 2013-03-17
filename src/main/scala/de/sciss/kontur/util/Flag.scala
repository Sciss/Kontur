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
  def update(value: Boolean) { this.value = value }
}