/*
 *  ObserverFrame.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.app.{ AbstractWindow, Document, DocumentEvent, DocumentListener }
import de.sciss.kontur.Main
import java.awt.{ BorderLayout, Dimension }
import javax.swing.{ JComponent, JTabbedPane, WindowConstants }
import javax.swing.event.{ ChangeEvent, ChangeListener }
import WindowConstants._

class ObserverFrame extends AppWindow( AbstractWindow.PALETTE )
with DocumentListener {

    private val ggTabPane = new JTabbedPane()
    private var mapTabs   = Map[ String, ObserverPage ]()
    private var shown: Option[ ObserverPage ] = None

    // ---- constructor ----
    {
        setTitle( getResourceString( "paletteObserver" ))
        setResizable( false )

//        ggTabPane.setPreferredSize( new Dimension( 400, 400 )) // XXX
        ggTabPane.addChangeListener( new ChangeListener {
            def stateChanged( e: ChangeEvent ) {
                val c = ggTabPane.getSelectedComponent
                val newShown  = if( c != null ) {
                  mapTabs.find( _._2.component == c ).map( _._2 )
                } else None
                if( newShown != shown ) {
                    shown.foreach( _.pageHidden() )
                    shown = newShown
                    shown.foreach( _.pageShown() )
                }
            }
        })
//        ggTabPane.putClientProperty( "JComponent.sizeVariant", "small" )
        getContentPane.add( ggTabPane, BorderLayout.CENTER )

        setDefaultCloseOperation( HIDE_ON_CLOSE )
        init()
        setSize( new Dimension( 300, 300 ))
        app.addComponent( Main.COMP_OBSERVER, this )
    }

    override protected def autoUpdatePrefs = true
	override protected def alwaysPackSize = false

    def addPage( page: ObserverPage ) {
       if( containsPage( page.id )) removePage( page.id )
       ggTabPane.addTab( page.title, page.component )
//       pack()
    }

    def removePage( id: String ) {
        mapTabs.get( id ).foreach( page => {
            mapTabs -= id
            ggTabPane.remove( page.component )
        })
    }

    def selectPage( id: String ) {
        mapTabs.get( id ).foreach( page => {
            ggTabPane.setSelectedComponent( page.component )
        })
    }

    def setPageEnabled( id: String, enabled: Boolean ) {
        mapTabs.get( id ).foreach( page => {
            val idx = ggTabPane.indexOfTabComponent( page.component )
            if( idx >= 0 ) ggTabPane.setEnabledAt( idx, enabled )
        })
    }

    def containsPage( id: String ) = mapTabs.contains( id )

    def getPage( id: String ) = mapTabs.get( id )

    // ---- DocumentListener interface ----
    
	def documentFocussed( e: DocumentEvent ) {
        val newDoc = e.getDocument
        mapTabs.foreach( entry => entry._2.documentChanged( newDoc ))
	}

	def documentAdded( e: DocumentEvent ) { /* ignore */ }
	def documentRemoved( e: DocumentEvent ) { /* ignore */ }
}

trait ObserverPage /* extends DynamicListening */ {
    def component:    JComponent
    def id:           String
    def title:        String
    def pageShown():  Unit
    def pageHidden(): Unit
    def documentChanged( newDoc: Document )
}

object DiffusionObserverPage {
    val id = "observer.diffusion"

/*
    lazy val instance: DiffusionObserverPage = {
        val page = new DiffusionObserverPage
        val app = AbstractApplication.getApplication()
        var ob = app.getComponent( Main.COMP_OBSERVER )
          .asInstanceOf[ ObserverFrame ]
        if( ob == null ) {
            ob = new ObserverFrame()
        }
        ob.addPage( page )
        page
    }
*/
}