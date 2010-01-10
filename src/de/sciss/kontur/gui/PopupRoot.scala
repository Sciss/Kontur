/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractWindow }
import de.sciss.gui.{ MenuGroup }
import javax.swing.{ Action, JComponent, JPopupMenu }

class PopupRoot extends MenuGroup( "root", null.asInstanceOf[ Action ]) {

	def createPopup( w: AbstractWindow ) : JPopupMenu =
      create( w ).asInstanceOf[ JPopupMenu ]

	override protected def createComponent( a: Action ) : JComponent =
      new JPopupMenu()
}
