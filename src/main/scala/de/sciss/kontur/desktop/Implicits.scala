package de.sciss.kontur
package desktop

import swing.{Component, Action}
import javax.{swing => j}
import j.KeyStroke
import java.awt.Dimension

object Implicits {
  implicit object DimensionPrefs extends Preferences.Type[Dimension] {
    private[desktop] def toString(value: Dimension): String = s"${value.width} ${value.height}"
    private[desktop] def valueOf(string: String): Option[Dimension] = try {
      val i = string.indexOf(' ')
      if (i < 0) return None
      val width   = string.substring(0, i).toInt
      val height  = string.substring(i + 1).toInt
      Some(new Dimension(width, height))
    }
    catch {
      case _: NumberFormatException => None
    }
  }

  implicit final class RichComponent(val component: Component) extends AnyVal {
    def addAction(action: Action, focus: FocusType = FocusType.Default) {
      val a       = action.peer
      val key     = a.getValue(j.Action.NAME).toString
      val stroke  = a.getValue(j.Action.ACCELERATOR_KEY).asInstanceOf[KeyStroke]
      component.peer.registerKeyboardAction(a, key, stroke, focus.id)
    }

    def removeAction(action: Action) {
      val a       = action.peer
      val stroke  = a.getValue(j.Action.ACCELERATOR_KEY).asInstanceOf[KeyStroke]
      component.peer.unregisterKeyboardAction(stroke)
    }
  }
}