package de.sciss.kontur.desktop

object Application {
}
trait Application {
  def quit(): Unit
}

trait SwingApplication extends Application {

}