/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 06.02.2010
 * Time: 17:35:23
 */
package de.sciss.kontur.gui

import javax.swing.{ JComponent }
import de.sciss.kontur.session.{ Diffusion, DiffusionFactory, MatrixDiffusion, Session }

object DiffusionGUIFactory {
   var registered = Map[ String, DiffusionGUIFactory ]()

   // ---- constructor ----
   // XXX eventually this needs to be decentralized
   registered += MatrixDiffusion.factoryName -> MatrixDiffusionGUI
}

trait DiffusionGUIFactory {
   def factory: DiffusionFactory
   type T <: JComponent
   def createPanel( doc: Session ) : T
   def fromPanel( panel: T ) : Option[ Diffusion ]
}