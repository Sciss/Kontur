/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import de.sciss.app.{ AbstractApplication, AbstractWindow }
import de.sciss.kontur.{ Main }
import javax.swing.{ WindowConstants }

class PrefsFrame extends AppWindow( AbstractWindow.SUPPORT ) {

    // ---- constructor ----
    {
      setTitle( "Preferences" ) // getResourceString( "framePrefs" )

	  val app = AbstractApplication.getApplication()
      
     // ---------- listeners ----------

      addListener( new AbstractWindow.Adapter() {
			override def windowClosing( e: AbstractWindow.Event ) {
				setVisible( false )
				dispose()
			}
      })

	   setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE )
	   init()
	   app.addComponent( Main.COMP_PREFS, this )
    }

}
