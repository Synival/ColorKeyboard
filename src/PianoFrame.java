import javax.swing.JFrame;
import javax.swing.event.*;
import java.awt.event.*;

public class PianoFrame extends JFrame
{
   private PianoPanel piano;
   public PianoFrame ()
   {
      // Initialize our frame.
      super ("Color Keyboard");
      setSize (800, 550);
      setResizable (false);

      // Add the piano to our frame.
      piano = new PianoPanel ();
      getContentPane().add (piano);

      // Make sure our keyboard is focused whenever we focus this window.
      addWindowListener (new WindowAdapter () {
         public void windowActivated (WindowEvent e)
            { focusKeyboard (); }
      });
   }

   public void focusKeyboard ()
   {
      piano.focusKeyboard ();
   }
}
