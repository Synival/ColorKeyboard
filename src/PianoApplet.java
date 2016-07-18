import javax.swing.JApplet;

public class PianoApplet extends JApplet
{
   private PianoPanel piano;
   public void init ()
   {
      piano = new PianoPanel ();
      getContentPane().add (piano);
      piano.focusKeyboard ();
   }
}
