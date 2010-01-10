/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.app.{ AbstractDocumentHandler }
import de.sciss.kontur.{ Main }
                        
class DocumentHandler( root: Main )
extends AbstractDocumentHandler( true ) {
//  	private val mapIDs = Map
/*
	def addDocument( Object source, Document doc ) {
		synchronized( this.sync ) {
			super.addDocument( source, doc );
			this.mapIDs.put( new Integer( ((Session) doc).getNodeID() ), doc );
		}
	}

	def removeDocument( Object source, Document doc ) {
		synchronized( this.sync ) {
			this.mapIDs.remove( new Integer( ((Session) doc).getNodeID() ));
			super.removeDocument( source, doc );
		}
	}
*/
}
