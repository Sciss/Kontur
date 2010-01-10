/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication }
import de.sciss.common.{ BasicApplication }
import java.awt.{ Dimension }
import java.util.{ StringTokenizer }

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
}