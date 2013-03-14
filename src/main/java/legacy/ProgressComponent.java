package legacy;

import java.awt.Component;
import java.awt.event.ActionListener;

/**
 *  An interface for classes
 *  that are capable of displaying
 *  progression information to the user
 *
 *  @author		Hanns Holger Rutz
 *  @version	0.61, 12-Oct-06
 */
public interface ProgressComponent
{
	public static final int	DONE		= 0;
	public static final int	FAILED		= 1;
	public static final int	CANCELLED	= 2;

	/**
	 *  Gets the component responsible
	 *  for displaying progression,
	 *  such as a ProgressBar.
	 */
	public Component getComponent();
	/**
	 *  Asks the component to reset
	 *  progression to zero at the
	 *  beginning of a process.
	 */
	public void resetProgression();
	/**
	 *  Asks the component to update
	 *  progression amount to the given value.
	 *
	 *  @param  p   the new progression amount between 0 and 1
	 */
	public void setProgression( float p );
	/**
	 *  Asks the component to indicate that the
	 *  progression is finished.
	 */
	public void finishProgression( int result );
	/**
	 *  Asks the component to display a custom
	 *  string describing the current process stage
	 *
	 *  @param  text	text to display in the progression component
	 */
	public void setProgressionText( String text );
	/**
	 *  Asks the component to display a message
	 *  related to the process.
	 *
	 *  @param  type	what type of message it is. Values are those
	 *					from JOptionPane : INFORMATION_MESSAGE, WARNING_MESSAGE
	 *					PLAIN_MESSAGE or ERROR_MESSAGE
	 *  @param  text	the message text to output
	 */
	public void showMessage( int type, String text );
	/**
	 *  Asks the component to display an error dialog.
	 *
	 *  @param  e			an <code>Exception</code> describing the error
	 *						which occured
	 *  @param  processName the name of the process in which the error
	 *						occured. this is usually used as a dialog's title string
	 */
	public void displayError( Exception e, String processName );

	public void addCancelListener( ActionListener l );
	public void removeCancelListener( ActionListener l );
}