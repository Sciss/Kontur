/*
 *  DiffusionGUIFactory.scala
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

package de.sciss.kontur.gui

import javax.swing.JComponent
import de.sciss.kontur.session.{ ConvolutionDiffusion, Diffusion, DiffusionFactory, MatrixDiffusion, Session }

object DiffusionGUIFactory {
   var registered = Map[ String, DiffusionGUIFactory ]()

   // ---- constructor ----
   // XXX eventually this needs to be decentralized
   registered += MatrixDiffusion.factoryName -> MatrixDiffusionGUI
   registered += ConvolutionDiffusion.factoryName -> ConvolutionDiffusionGUI
}

trait DiffusionGUIFactory {
   def factory: DiffusionFactory
   type T <: JComponent
   def createPanel( doc: Session ) : T
   def fromPanel( panel: T ) : Option[ Diffusion ]
}