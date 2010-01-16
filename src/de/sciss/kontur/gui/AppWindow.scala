/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication }
import de.sciss.common.{ BasicApplication }
import java.awt.{ Container, Dimension }
import java.io.{ File }
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
       putRootPaneProperty( "apple.awt.brushMetalLook", java.lang.Boolean.TRUE )
    }

  protected def setWindowFile( f: File ) {
    putRootPaneProperty( "Window.documentFile", f )
  }

  private def putRootPaneProperty( name: String, value: AnyRef ) {
      getWindow match {
        case rpc: RootPaneContainer =>
          rpc.getRootPane().putClientProperty( name, value )
        case _ =>
      }
  }

  protected def setAlpha( amount: Float ) {
       putRootPaneProperty( "Window.alpha", new java.lang.Float( amount ))
       putRootPaneProperty( "apple.awt.draggableWindowBackground", java.lang.Boolean.FALSE )
  }

/*  protected def setContentPane( c: Container ) {
     getWindow match {
       case f: JFrame => f.setContentPane( c )
       case f: JInternalFrame => f.setContentPane( c )
     }
  }
  */
}