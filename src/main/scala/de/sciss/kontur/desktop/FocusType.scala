package de.sciss.kontur.desktop

import javax.swing.JComponent

object FocusType {
  case object Default   extends FocusType { def id = JComponent.WHEN_FOCUSED }
  case object Window    extends FocusType { def id = JComponent.WHEN_IN_FOCUSED_WINDOW }
  case object Ancestor  extends FocusType { def id = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT }
}
sealed trait FocusType { def id: Int }