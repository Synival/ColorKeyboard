import javax.swing.JApplet;

public class PianoApplet extends JApplet
{
   private PianoPanel piano;
   public void init ()
   {
      // Add a piano and focus the keys automatically.
      piano = new PianoPanel ();
      getContentPane().add (piano);
      piano.focusKeyboard ();
   }
}
