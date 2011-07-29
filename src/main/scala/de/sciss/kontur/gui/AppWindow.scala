/*
 *  AppWindow.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.gui

import de.sciss.app.AbstractApplication
import de.sciss.common.BasicApplication
import java.awt.Dimension
import java.io.File
import java.util.StringTokenizer
import javax.swing.RootPaneContainer

class AppWindow( mode: Int ) extends de.sciss.common.AppWindow( mode ) {
   protected val app				   = AbstractApplication.getApplication.asInstanceOf[ BasicApplication ]
	protected val internalFrames	= app.getWindowHandler.usesInternalFrames
    
  	protected def getResourceString( key: String ) : String =
		app.getResourceString( key )

   protected def stringToDimension( value: String ) : Dimension = {
      if( value == null ) return null
      val tok	= new StringTokenizer( value )
      new Dimension( Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ))
	}

   protected def makeUnifiedLook() {
      putRootPaneProperty( "apple.awt.brushMetalLook", java.lang.Boolean.TRUE )
   }

   protected def setWindowFile( f: File ) {
      putRootPaneProperty( "Window.documentFile", f )
   }

   private def putRootPaneProperty( name: String, value: AnyRef ) {
      getWindow match {
         case rpc: RootPaneContainer => rpc.getRootPane.putClientProperty( name, value )
         case _ =>
      }
   }

   protected def setAlpha( amount: Float ) {
      putRootPaneProperty( "Window.alpha", new java.lang.Float( amount ))
      putRootPaneProperty( "apple.awt.draggableWindowBackground", java.lang.Boolean.FALSE )
   }

/* protected def setContentPane( c: Container ) {
      getWindow match {
         case f: JFrame => f.setContentPane( c )
         case f: JInternalFrame => f.setContentPane( c )
      }
   }
*/
}