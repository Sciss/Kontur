/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication }
import de.sciss.common.{ BasicApplication }
import java.awt.{ Container, Dimension }
import java.util.{ StringTokenizer }
import javax.swing.{ JFrame, JInternalFrame, RootPaneContainer }

class AppWindow( mode: Int ) extends de.sciss.common.AppWindow( mode ) {
    protected val app				= AbstractApplication.getApplication().asInstanceOf[ BasicApplication ]
	protected val internalFrames	= app.getWindowHandler().usesInternalFrames()
    
  	protected def getResourceString( key: String ) : String =
		app.getResourceString( key )

    protected def stringToDimension( value: String ) : Dimension = {
        if( value == null ) return null
        val tok	= new StringTokenizer( value )
        new Dimension( Integer.parseInt( tok.nextToken() ), Integer.parseInt( tok.nextToken() ))
	}

    protected def makeUnifiedLook {
      getWindow match {
        case rpc: RootPaneContainer => {
          rpc.getRootPane().putClientProperty( "apple.awt.brushMetalLook", java.lang.Boolean.TRUE )
        }
        case _ =>
      }
    }

  protected def setAlpha( amount: Float ) {
      getWindow match {
        case rpc: RootPaneContainer => {
          val rp = rpc.getRootPane()
          rp.putClientProperty( "Window.alpha", new java.lang.Float( amount ))
          rp.putClientProperty( "apple.awt.draggableWindowBackground", java.lang.Boolean.FALSE )
        }
        case _ =>
      }
  }

/*  protected def setContentPane( c: Container ) {
     getWindow match {
       case f: JFrame => f.setContentPane( c )
       case f: JInternalFrame => f.setContentPane( c )
     }
  }
  */
}